from app.parsers import dou

PARSERS = {
    "DOU_JAVA": (dou.parse, dou.VERSION),
    "DOU_MARKETING": (dou.parse, dou.VERSION),
}
