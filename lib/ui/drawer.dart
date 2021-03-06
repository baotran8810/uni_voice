import 'package:flutter/material.dart';
import 'package:uni_voice/ListScreen.dart';
import 'package:uni_voice/main.dart';

Widget drawer(BuildContext context) {
  return Drawer(
    // Add a ListView to the drawer. This ensures the user can scroll
    // through the options in the drawer if there isn't enough vertical
    // space to fit everything.
    child: ListView(
      // Important: Remove any padding from the ListView.
      padding: EdgeInsets.zero,
      children: <Widget>[
        DrawerHeader(
          child: Text('CameraUV'),
          decoration: BoxDecoration(
            color: Colors.blue,
          ),
        ),
        ListTile(
          title: Text('Scan'),
          onTap: () {
            Navigator.push(
                context, MaterialPageRoute(builder: (context) => MyHomePage()));
          },
        ),
        ListTile(
          title: Text('List'),
          onTap: () {
            Navigator.push(
                context, MaterialPageRoute(builder: (context) => ListScreen()));
          },
        ),
      ],
    ),
  );
}
