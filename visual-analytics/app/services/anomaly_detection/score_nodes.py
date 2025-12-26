from typing import List, Dict


def attach_scores(
    nodes: List[Dict],
    anomaly_scores: List[Dict]
) -> List[Dict]:
    """
    Attaches anomaly scores to enriched node data.

    Input:
        nodes           → output of /backend/api/nodes/enriched
        anomaly_scores  → output of run_isolation_forest()

    Output:
        nodes_scored → payload ready for
        POST /backend/api/nodes/scored/batch
    """

    if not nodes:
        return []

    # Create fast lookup: node_id -> score dict
    score_map = {
        s["node_id"]: s
        for s in anomaly_scores
    }

    nodes_scored = []

    for node in nodes:
        node_id = node.get("node_id")

        score = score_map.get(node_id, {
            "anomaly_score": None,
            "is_anomalous": 0
        })

        # Merge dictionaries (node fields + score fields)
        merged = {
            **node,
            "anomaly_score": score.get("anomaly_score"),
            "is_anomalous": score.get("is_anomalous")
        }

        nodes_scored.append(merged)

    return nodes_scored

def score_single_node(enriched_node: Dict) -> float:
    """
    Scores ONE node.
    Wrapper added for node-triggered ML pipeline.
    """

    # Import here to avoid circular imports
    from app.services.anomaly_detection.eif_detector import run_isolation_forest

    # EIF expects a list → wrap single node
    scores = run_isolation_forest([enriched_node])

    if not scores:
        return 0.0

    return scores[0].get("anomaly_score", 0.0)
