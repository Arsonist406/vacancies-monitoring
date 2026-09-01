from app.parsers import djinni, dou

PARSERS = {
    "DOU": (dou.parse, dou.VERSION),
    "DJINNI": (djinni.parse, djinni.VERSION)
}
