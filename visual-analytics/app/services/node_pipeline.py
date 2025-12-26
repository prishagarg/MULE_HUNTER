from typing import List

from app.services.anomaly_detection.score_nodes import score_single_node
from app.services.explainability.explanation_mapper import build_fraud_explanation
from app.services.explainability.shap_runner import run_shap
from app.clients.backend_client import (
    fetch_node_enriched,
    post_anomaly_score,
    post_fraud_explanation,
    post_shap_explanation,
)


def normalize_enriched_node(enriched: dict) -> dict:
    return {
        # keep both for safety
        "nodeId": enriched.get("nodeId"),
        "node_id": enriched.get("nodeId"),

        "in_degree": enriched.get("inDegree"),
        "out_degree": enriched.get("outDegree"),

        "total_incoming": enriched.get("totalIncoming"),
        "total_outgoing": enriched.get("totalOutgoing"),

        "risk_ratio": enriched.get("riskRatio"),
        "tx_velocity": enriched.get("txVelocity"),

        "account_age_days": enriched.get("accountAgeDays"),
        "balance": enriched.get("balance"),
    }


async def run_node_pipeline(nodes: List):
    """
    Runs ML ONLY for the given nodes.
    Triggered by backend per transaction.
    """

    for node in nodes:
        node_id = node.nodeId

        try:
            # 1️⃣ Fetch node features
            raw_node = await fetch_node_enriched(node_id)
            if not raw_node:
                continue

            enriched_node = normalize_enriched_node(raw_node)

            if not enriched_node:
                continue

            # 2️⃣ Anomaly detection
            score = score_single_node(enriched_node)

            # 3️⃣ Explainability
            reasons = build_fraud_explanation(enriched_node, score)

            # IMPORTANT FIX: wrap single node as list
            shap_results = run_shap([
                {
                    **enriched_node,
                    "is_anomalous": 1 if score > 0.5 else 0,
                    "anomaly_score": score,
                }
            ])


            # 4️⃣ Persist results
            await post_anomaly_score(node_id, score)
            await post_fraud_explanation(node_id, reasons)

            if shap_results:
                await post_shap_explanation(shap_results)

            print(f"✅ Visual ML completed for node {node_id}")

        except Exception as e:
            print(f"❌ Visual ML failed for node {node_id}: {str(e)}")
