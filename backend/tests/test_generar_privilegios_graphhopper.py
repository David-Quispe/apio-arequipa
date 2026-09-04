from scripts.generar_privilegios_graphhopper import boost_desde_score


def test_boost_score_cero_no_da_ventaja():
    assert boost_desde_score(0) == 1.0


def test_boost_score_maximo_es_40_por_ciento():
    assert boost_desde_score(10) == 1.4


def test_boost_es_lineal():
    assert boost_desde_score(5) == 1.2
