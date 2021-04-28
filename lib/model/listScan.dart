class Scan {
  String content;

  String date;

  int pos;

  Scan(this.content, this.date, this.pos);

  toJSONEncodable() {
    Map<String, dynamic> m = new Map();
    m['content'] = content;
    m['date'] = date;
    m['pos'] = pos;
    return m;
  }
}

class ListScan {
  List<Scan> items;

  ListScan() {
    items = new List();
  }
  toJSONEncodable() {
    return items.map((item) {
      return item.toJSONEncodable();
    }).toList();
  }
}
