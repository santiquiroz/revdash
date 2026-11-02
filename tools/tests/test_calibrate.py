import pytest

from tools.calibrate_speed import CalibrationError, best_offset, fit_factor, load_revscope_speed, resample


def test_fit_factor_recovers_known_k():
    hz = [0, 10, 20, 30, 40]
    k, r2 = fit_factor(hz, [h * 1.8 for h in hz])
    assert abs(k - 1.8) < 1e-9 and r2 > 0.999


def test_best_offset_finds_shift():
    a = [float((i * i) % 17) for i in range(200)]
    b = a[3:] + [0.0, 0.0, 0.0]
    assert best_offset(a, b, max_shift=10) == 3


def test_constant_series_raises():
    with pytest.raises(CalibrationError):
        best_offset([1.0] * 200, [1.0] * 200, max_shift=10)


def test_all_zero_hz_raises():
    with pytest.raises(CalibrationError):
        fit_factor([0.0] * 100, [10.0] * 100)


def test_load_revscope_speed_reads_only_0d(tmp_path):
    p = tmp_path / "t.csv"
    p.write_text("section,timestamp,key,value1,value2\nobd,1000,0D,42.0,\nobd,1000,0C,3000,\ngps,1000,6.2,-75.5,40\n")
    assert load_revscope_speed(p) == [(1000, 42.0)]


def test_resample_holds_last_value():
    assert resample([(0, 1.0), (1200, 3.0)], 500) == [1.0, 1.0, 1.0]


def test_resample_single_point_returns_single_value():
    assert resample([(100, 2.5)], 500) == [2.5]


def test_best_offset_short_overlap_raises():
    with pytest.raises(CalibrationError):
        best_offset([float(i) for i in range(20)], [float(i) for i in range(20)], max_shift=10)
