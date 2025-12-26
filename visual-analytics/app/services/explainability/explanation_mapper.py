# explanation_mapper.py
# Maps ML features to human-understandable fraud explanations

FEATURE_EXPLANATIONS = {
    "in_degree": {
        "positive": "Received money from an unusually large number of accounts",
        "negative": "Normal number of incoming transactions"
    },
    "out_degree": {
        "positive": "Sent money to many different accounts in a short time",
        "negative": "Normal outgoing transaction behavior"
    },
    "total_incoming": {
        "positive": "Received unusually high total amount of money",
        "negative": "Incoming transaction volume appears normal"
    },
    "total_outgoing": {
        "positive": "Transferred large sums of money outward rapidly",
        "negative": "Outgoing transaction volume appears normal"
    },
    "risk_ratio": {
        "positive": "High-risk transaction pattern detected (possible mule behavior)",
        "negative": "Risk indicators within safe range"
    }
}

def build_fraud_explanation(enriched_node: dict, score: float):
    """
    Build human-readable fraud explanations for ONE node
    using FEATURE_EXPLANATIONS.
    """

    reasons = []

    for feature, explanation in FEATURE_EXPLANATIONS.items():
        value = enriched_node.get(feature)

        if value is None:
            continue

        # Simple heuristic (demo-safe)
        if value > 0:
            reasons.append(explanation["positive"])
        else:
            reasons.append(explanation["negative"])

    # Fallback (important)
    if not reasons:
        reasons.append("No abnormal behavior detected")

    # Optional: add score-based reason
    if score > 0.8:
        reasons.append("High overall anomaly score")

    return reasons

