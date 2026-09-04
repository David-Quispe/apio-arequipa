from scripts.clasificar_privilegios_especificos import score_contraflujo


def test_separador_fisico_dificulta_contraflujo():
    # separador central fisico (ej. Av. Ejercito): cruzar solo es posible
    # en huecos del separador, score bajo aunque sea de sentido unico
    assert score_contraflujo("fisico", True) == 3.0
    assert score_contraflujo("fisico", False) == 3.0


def test_sentido_unico_sin_separador_es_el_mejor_caso():
    # sin separador y de sentido unico: el carril contrario esta vacio de
    # trafico legitimo -- el escenario mas favorable para contraflujo
    assert score_contraflujo("ninguno", True) == 9.0
    assert score_contraflujo("linea_pintada", True) == 9.0


def test_doble_sentido_sin_separador_es_intermedio():
    # sin separador pero doble sentido: hay que invadir un carril con
    # trafico circulando en el otro sentido
    assert score_contraflujo("ninguno", False) == 6.0


def test_sin_dato_de_separador_no_inventa_valor():
    assert score_contraflujo(None, True) is None
