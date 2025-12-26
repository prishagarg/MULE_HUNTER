import logging
import httpx
from app.config import BACKEND_BASE_URL, REQUEST_TIMEOUT


# =============================
# INTERNAL SAFE POST
# =============================

async def _post_safe(url: str, data):
    """
    Async backend POST with timeout + error isolation.
    Never crashes pipeline.
    """
    try:
        async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
            await client.post(url, json=data)
            logging.info(f"POST success → {url}")
    except Exception as e:
        logging.error(f"POST failed → {url} | {e}")


# =============================
# READ (NODE-SCOPED)
# =============================

async def fetch_node_enriched(node_id: int):
    """
    Fetch enriched data for ONE node.
    """
    async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
        resp = await client.get(
            f"{BACKEND_BASE_URL}/backend/api/nodes/enriched/{node_id}"
        )

        if not resp.is_success:
            logging.error(
                f"Failed to fetch node {node_id} "
                f"[{resp.status_code}]: {resp.text}"
            )
            return None

        return resp.json()


# =============================
# WRITE (NODE-SCOPED)
# =============================

async def post_anomaly_score(node_id: int, score: float):
    await _post_safe(
        f"{BACKEND_BASE_URL}/backend/api/visual/anomaly-scores/batch",
    [

        {
            "nodeId": node_id,
            "anomalyScore": score,
            "model": "eif_v1",
            "source": "visual-analytics"
        }
    ]
)

async def post_shap_explanation(node_id: int, shap_values: dict):
    await _post_safe(
    f"{BACKEND_BASE_URL}/backend/api/visual/shap-explanations/batch",
    [
        {
            "nodeId": node_id,
            "anomalyScore": None,
            "topFactors": shap_values,
            "model": "shap_v1",
            "source": "visual-analytics"
        }
    ]
)



async def post_fraud_explanation(node_id: int, reasons: list):
    await _post_safe(
        f"{BACKEND_BASE_URL}/backend/api/visual/fraud-explanations/batch",
        {
            "nodeId": node_id,
            "reasons": reasons,
            "model": "rules_v1",
            "source": "visual-analytics"
        }
    )
