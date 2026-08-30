from app.parsers import dou

PARSERS = {
    "DOU_JAVA": (dou.parse, dou.VERSION),
    "DOU_GENERAL": (dou.parse, dou.VERSION),
}
