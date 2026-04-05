import os
from sqlalchemy import create_engine, text
import pandas as pd
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://asset_radar:asset_radar@localhost:5433/asset_radar"
)

_engine = None


def get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(DATABASE_URL)
    return _engine


def load_prices(symbol=None, source=None, from_dt=None, to_dt=None):
    """asset_price_history 테이블에서 가격 데이터를 DataFrame으로 로드한다."""
    query = "SELECT symbol, quote_currency, source, price, signed_change_rate, collected_at FROM asset_price_history WHERE 1=1"
    params = {}

    if symbol:
        query += " AND symbol = :symbol"
        params["symbol"] = symbol
    if source:
        query += " AND source = :source"
        params["source"] = source
    if from_dt:
        query += " AND collected_at >= :from_dt"
        params["from_dt"] = from_dt
    if to_dt:
        query += " AND collected_at <= :to_dt"
        params["to_dt"] = to_dt

    query += " ORDER BY collected_at ASC"

    with get_engine().connect() as conn:
        df = pd.read_sql(text(query), conn, params=params)

    if not df.empty:
        df["collected_at"] = pd.to_datetime(df["collected_at"], utc=True)
        df["price"] = df["price"].astype(float)
        df["signed_change_rate"] = df["signed_change_rate"].astype(float)

    return df


def load_all_prices(from_dt=None, to_dt=None):
    """모든 자산의 가격 데이터를 한번에 로드한다."""
    return load_prices(from_dt=from_dt, to_dt=to_dt)


def get_asset_list():
    """수집 중인 자산 목록(source, symbol, quote_currency)을 반환한다."""
    query = """
        SELECT DISTINCT source, symbol, quote_currency,
               COUNT(*) as row_count,
               MIN(collected_at) as first_collected,
               MAX(collected_at) as last_collected
        FROM asset_price_history
        GROUP BY source, symbol, quote_currency
        ORDER BY source, symbol
    """
    with get_engine().connect() as conn:
        return pd.read_sql(text(query), conn)
