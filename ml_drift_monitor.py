import argparse
import json
import subprocess
import sys
from pathlib import Path

import pandas as pd

FEATURE_COLUMNS = [
    "requestCount",
    "failureRate",
    "distinctUrlCount",
    "averageUrlLength",
    "hourOfDay",
]


def load_feature_frame(csv_path: Path) -> pd.DataFrame:
    df = pd.read_csv(csv_path)
    missing = [c for c in FEATURE_COLUMNS if c not in df.columns]
    if missing:
        raise ValueError(f"Missing feature columns: {missing}")
    return df[FEATURE_COLUMNS]


def calc_stats(df: pd.DataFrame) -> dict:
    stats = {}
    for col in FEATURE_COLUMNS:
        stats[col] = {
            "mean": float(df[col].mean()),
            "std": float(df[col].std(ddof=0)) if len(df) > 1 else 0.0,
        }
    return stats


def calc_drift_score(current: dict, baseline: dict) -> tuple[float, dict]:
    details = {}
    scores = []

    for col in FEATURE_COLUMNS:
        c = current[col]
        b = baseline[col]
        denom = b["std"] if b["std"] > 1e-9 else 1.0
        z_shift = abs(c["mean"] - b["mean"]) / denom
        details[col] = {
            "z_shift": z_shift,
            "current_mean": c["mean"],
            "baseline_mean": b["mean"],
            "baseline_std": b["std"],
        }
        scores.append(z_shift)

    drift_score = float(sum(scores) / len(scores)) if scores else 0.0
    return drift_score, details


def save_baseline(path: Path, stats: dict) -> None:
    path.write_text(json.dumps(stats, indent=2), encoding="utf-8")


def load_baseline(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def maybe_retrain(enabled: bool) -> int:
    if not enabled:
        return 0

    print("[ACTION] Drift threshold exceeded. Start retraining...")
    completed = subprocess.run([sys.executable, "ml_training.py"], check=False)
    return completed.returncode


def main() -> int:
    parser = argparse.ArgumentParser(description="Drift monitor and optional retrain trigger")
    parser.add_argument("--feature-csv", default="feature-data.csv")
    parser.add_argument("--baseline", default="data/drift-baseline.json")
    parser.add_argument("--threshold", type=float, default=1.5)
    parser.add_argument("--init-baseline", action="store_true")
    parser.add_argument("--auto-retrain", action="store_true")
    args = parser.parse_args()

    feature_csv = Path(args.feature_csv)
    baseline_path = Path(args.baseline)
    baseline_path.parent.mkdir(parents=True, exist_ok=True)

    current_df = load_feature_frame(feature_csv)
    current_stats = calc_stats(current_df)

    if args.init_baseline or not baseline_path.exists():
        save_baseline(baseline_path, current_stats)
        print(f"[INIT] Baseline saved: {baseline_path}")
        return 0

    baseline_stats = load_baseline(baseline_path)
    drift_score, details = calc_drift_score(current_stats, baseline_stats)

    print(f"[DRIFT] score={drift_score:.3f}, threshold={args.threshold:.3f}")
    for col in FEATURE_COLUMNS:
        print(f"  - {col}: z_shift={details[col]['z_shift']:.3f}")

    if drift_score >= args.threshold:
        print("[DRIFT] Drift detected")
        rc = maybe_retrain(args.auto_retrain)
        return rc

    print("[DRIFT] No significant drift")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
