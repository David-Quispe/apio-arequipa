from app.services.traffic import factor_ajuste_eta, nivel


def test_nivel_umbrales():
    assert nivel(1.0) == "baja"
    assert nivel(0.75) == "baja"
    assert nivel(0.74) == "media"
    assert nivel(0.5) == "media"
    assert nivel(0.49) == "alta"
    assert nivel(0.0) == "alta"


def test_factor_ajuste_eta_sin_congestion():
    assert factor_ajuste_eta(1.0) == 1.0


def test_factor_ajuste_eta_acotado_a_2x():
    # ratio muy bajo (mucha congestion) no debe disparar el ETA mas de 2x
    assert factor_ajuste_eta(0.1) == 2.0


def test_factor_ajuste_eta_datos_invalidos():
    # ratio 0 o negativo (dato invalido/sin_datos) no debe dividir por cero
    assert factor_ajuste_eta(0) == 1.0
    assert factor_ajuste_eta(-1) == 1.0


def test_factor_ajuste_eta_es_inverso_al_ratio():
    assert factor_ajuste_eta(0.5) == 2.0
