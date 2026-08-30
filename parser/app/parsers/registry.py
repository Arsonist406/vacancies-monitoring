from app.parsers import djinni, dou

PARSERS = {
    "DOU_JAVA": (dou.parse, dou.VERSION),
    "DOU_GENERAL": (dou.parse, dou.VERSION),
    "DJINNI_JAVA": (djinni.parse, djinni.VERSION),
    "DJINNI_GENERAL": (djinni.parse, djinni.VERSION),
}
