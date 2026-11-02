from tools.rdp import Frame, frequency_series, parse_line


def test_parses_edge():
    assert parse_line("E,1234,3,1") == Frame("E", 1234, ("3", "1"))


def test_ignores_terminal_prefix():
    assert parse_line("12:01:02.345 F,500,0,104000") == Frame("F", 500, ("0", "104000"))


def test_ignores_capture_prefix():
    assert parse_line("1792434050123 S,5000,0") == Frame("S", 5000, ("0",))


def test_garbage_and_truncated_lines_are_none():
    for line in ["", "\x00\xffE,12", "F,500,0", "Z,1,2", "E,abc,1,1"]:
        assert parse_line(line) is None


def test_can_with_empty_data():
    assert parse_line("C,10,18DAF110,x,") == Frame("C", 10, ("18DAF110", "x", ""))


def test_frequency_series_filters_channel_and_converts_hz():
    frames = [parse_line("F,0,0,104000"), parse_line("F,0,1,5000"), parse_line("F,250,0,0")]
    assert frequency_series(frames, 0) == [(0, 104.0), (250, 0.0)]


def test_frequency_series_empty_when_channel_has_no_data():
    frames = [parse_line("F,0,1,5000"), parse_line("F,250,1,0")]
    assert frequency_series(frames, 0) == []
