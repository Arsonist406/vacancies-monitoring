from app.parsers import dou

PARSERS = {
    "DOU": (dou.parse, dou.VERSION),
}
