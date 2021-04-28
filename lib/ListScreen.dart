import 'package:flutter/material.dart';
//import 'package:localstorage/localstorage.dart';
import 'package:uni_voice/ReadScreen.dart';
import 'package:uni_voice/model/listScan.dart';
import "package:uni_voice/main.dart";

class ListScreen extends StatefulWidget {
  @override
  _ListScreenState createState() => _ListScreenState();
}

class _ListScreenState extends State<ListScreen> {
  bool initialized = false;
  final ListScan listScan = new ListScan();
  //final LocalStorage storage = new LocalStorage("UV_app");

  String detectType(String a) {
    if (a.contains("<tag")) {
      return "NAVI";
    } else {
      return "DOC";
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: new IconButton(
          icon: new Icon(Icons.arrow_back, color: Colors.black),
          onPressed: () => Navigator.of(context).pop(),
        ),
        centerTitle: true,
        title: Text(
          "List Scan",
          style: TextStyle(color: Colors.black),
        ),
        backgroundColor: Colors.white,
      ),
      body: Container(
          padding: EdgeInsets.all(10.0),
          constraints: BoxConstraints.expand(),
          child: FutureBuilder(
              future: storage.ready,
              builder: (BuildContext context, AsyncSnapshot snapshot) {
                if (snapshot.data == null) {
                  return Center(
                    child: CircularProgressIndicator(),
                  );
                }
                if (!initialized) {
                  var items = storage.getItem("UV");

                  if (items != null) {
                    listScan.items = List<Scan>.from((items as List).map(
                        (item) =>
                            Scan(item['content'], item['date'], item['pos'])));
                  }
                  print(listScan.items.length);
                  initialized = true;
                }

                return ListView.builder(
                  itemCount: listScan.items.length,
                  itemBuilder: (context, index) {
                    return GestureDetector(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (detectType(listScan.items[index].content) ==
                              "DOC")
                            Padding(
                              padding: const EdgeInsets.all(5.0),
                              child: Text("Document Code"),
                            )
                          else
                            Padding(
                              padding: const EdgeInsets.all(5.0),
                              child: Text("Navi Code"),
                            ),
                          Padding(
                            padding: const EdgeInsets.all(5.0),
                            child: Text(
                              listScan.items[index].content.substring(0, 10),
                              style: TextStyle(
                                  fontWeight: FontWeight.w600, fontSize: 20),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.all(8.0),
                            child: Text(listScan.items[index].date),
                          ),
                          Padding(
                            padding: EdgeInsets.all(10),
                            child: Container(
                              height: 1.0,
                              width: MediaQuery.of(context).size.width,
                              color: Colors.black45,
                            ),
                          ),
                        ],
                      ),
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) =>
                                ReadScreen(scan: listScan.items[index]),
                          ),
                        );
                      },
                    );
                  },
                );
              })),
    );
  }
}
