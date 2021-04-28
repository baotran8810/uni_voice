import 'package:flutter/material.dart';

class MenuScreen extends StatefulWidget {
  @override
  _MenuScreenState createState() => _MenuScreenState();
}

class _MenuScreenState extends State<MenuScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: Center(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          GestureDetector(
              child: Container(
            color: Colors.blue,
            child: Icon(
              Icons.list,
              color: Colors.white,
              size: 50,
            ),
          )),
          GestureDetector(
              child: Container(
            color: Colors.green,
            child: Icon(
              Icons.book,
              color: Colors.white,
              size: 50,
            ),
          )),
        ],
      ),
    ));
  }
}
