import 'dart:core';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:localstorage/localstorage.dart';
import 'package:uni_voice/model/listScan.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:uni_voice/ui/MenuScreen.dart';
import 'dart:io' show Platform;
import 'ListScreen.dart';
import 'ReadScreen.dart';

final LocalStorage storage = LocalStorage("UV_app");
FlutterTts flutterTts;
List<CameraDescription> cameras;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  cameras = await availableCameras();
  runApp(MyApp());
}
double tl_x, tl_y, tr_x, tr_y, bl_x, bl_y, br_x, br_y;
String result = "";
double width, height;
int pWidth = 400;
int pHeight = 600;
int videoW = 480;
int videoH = 640;
bool send = false;
class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {

    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  @override
  _MyHomePageState createState() => _MyHomePageState();
}

enum TtsState { playing, stopped, paused, continued }

class _MyHomePageState extends State<MyHomePage> {
  String decode = '';
  bool initialized = false;
  int length = 0;
  final ListScan listScan = new ListScan();
  final ListScan temp = new ListScan();
  List<String> temp1 = [];
  CameraController cameraController;

  TtsState ttsState = TtsState.stopped;

  get isPlaying => ttsState == TtsState.playing;
  get isStopped => ttsState == TtsState.stopped;
  get isPaused => ttsState == TtsState.paused;
  get isContinued => ttsState == TtsState.continued;

  bool get isIOS => !kIsWeb && Platform.isIOS;
  bool get isAndroid => !kIsWeb && Platform.isAndroid;
  bool get isWeb => kIsWeb;

  initTts() {
    flutterTts = FlutterTts();

    if (isAndroid) {
      _getEngines();
    }

    flutterTts.setStartHandler(() {
      setState(() {
        print("Playing");
        ttsState = TtsState.playing;
      });
    });

    flutterTts.setCompletionHandler(() {
      setState(() {
        print("Complete");
        ttsState = TtsState.stopped;
      });
    });

    flutterTts.setCancelHandler(() {
      setState(() {
        print("Cancel");
        ttsState = TtsState.stopped;
      });
    });

    if (isWeb || isIOS) {
      flutterTts.setPauseHandler(() {
        setState(() {
          print("Paused");
          ttsState = TtsState.paused;
        });
      });

      flutterTts.setContinueHandler(() {
        setState(() {
          print("Continued");
          ttsState = TtsState.continued;
        });
      });
    }

    flutterTts.setErrorHandler((msg) {
      setState(() {
        print("error: $msg");
        ttsState = TtsState.stopped;
      });
    });
  }

  String getDate() {
    return DateTime.now().day.toString() +
        "/" +
        DateTime.now().month.toString() +
        "/" +
        DateTime.now().year.toString();
  }

  _addItem(String content) {
    setState(() {
      final item = new Scan(content, getDate(), length);
      listScan.items.add(item);
      _saveToStorage();
      _saveAudio(content, length);
      length++;
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => ReadScreen(scan: item),
        ),
      );
    });
  }

  Future _getEngines() async {
    var engines = await flutterTts.getEngines;
    if (engines != null) {
      for (dynamic engine in engines) {
        print(engine);
      }
    }
  }

  Future _saveAudio(String content, int pos) async {
    List<String> temp = content.split("。");
    for (var i = 0; i < temp.length; i++) {
      print(i);
      await flutterTts.synthesizeToFile(temp[i], "audio${pos}_$i.wav");
    }
  }

  void _saveToStorage() {
    setState(() {
      storage.setItem("UV", listScan.toJSONEncodable());
    });
  }

  String detectType(String a) {
    if (a.contains("<tag")) {
      return "NAVI";
    }
    return "DOC";
  }



  @override
  void initState() {
    super.initState();
    initTts();
    send = false;
    cameraController = CameraController(cameras[0], ResolutionPreset.high);
    cameraController.initialize().then((_)  {
      if (!mounted) {
        return;
      }
      setState(() {});
     cameraController.startImageStream((image) {
        setState(() {
          tl_x = image.tl_x;
          tl_y = image.tl_y;
          tr_x = image.tr_x;
          tr_y = image.tr_y;
          br_x = image.br_x;
          br_y = image.br_y;
          bl_x = image.bl_x;
          bl_y = image.bl_y;
          print(image.result);
          if(image.result != "-1" && image.result != null && send == false){
            _addItem(image.result);
            send = true;
          }
        });
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    width = MediaQuery.of(context).size.width;
    height = MediaQuery.of(context).size.height;
    final deviceRatio = width /height;
    return Scaffold(
      appBar: AppBar(
          backgroundColor: Colors.white,
          title: Text(
            "UniVoice",
            style: TextStyle(
              color: Colors.black,
            ),
          ),
          centerTitle: true,
          actions: <Widget>[
            IconButton(
              icon: const Icon(
                Icons.list_alt_rounded,
                color: Colors.black,
              ),
              tooltip: 'Show Snackbar',
              onPressed: () {
                Navigator.push(context,
                    MaterialPageRoute(builder: (context) => ListScreen()));
              },
            ),
          ]),
      //drawer: drawer(context),
      body: Stack(
      children: [
        if (cameraController == null || !cameraController.value.isInitialized) ...{
        Text(
          'Wait for camera',
          style: TextStyle(
            color: Colors.black,
            fontSize: 24.0,
            fontWeight: FontWeight.w900,
          ),
        )
        }
        else ...{
              CameraPreview(cameraController),
              CustomPaint(painter: ShapePainter()),
        }
    ],
    ));

    // FutureBuilder(
      //     future: storage.ready,
      //     builder: (BuildContext context, AsyncSnapshot snapshot) {
      //       if (snapshot.data == null) {
      //         return Center(
      //           child: CircularProgressIndicator(),
      //         );
      //       }
      //       if (!initialized) {
      //         var items = storage.getItem("UV");
      //
      //         if (items != null) {
      //           listScan.items = List<Scan>.from((items as List).map((item) =>
      //               Scan(item['content'], item['date'], item['pos'])));
      //         }
      //         // initTts();
      //         print(listScan.items.length);
      //         length = listScan.items.length;
      //         initialized = true;
      //       }
      //       return Stack(
      //           children: <Widget>[
      //                 if (cameraController == null || !cameraController.value.isInitialized) ...{
      //                     Text(
      //                       'Wait for camera',
      //                       style: TextStyle(
      //                     color: Colors.black,
      //                     fontSize: 24.0,
      //                     fontWeight: FontWeight.w900,
      //                     ),
      //                     )
      //                 } else ...{
      //                   CameraPreview(cameraController),
      //                   CustomPaint(painter: ShapePainter()),
      //                 }
      //                 ]);
      //     }),

  }
}

class ShapePainter extends CustomPainter {

  @override
  void paint(Canvas canvas, Size size) {
    var paint = Paint()
      ..color = Colors.teal
      ..strokeWidth = 5
      ..strokeCap = StrokeCap.round;
    pHeight = 720;
    pWidth = 480;
    width = 360;
    height = 480;
    print("Size canvas:" +size.height.toString() + "x" +  size.width.toString());
    print("Screen Size:" + height.toString() + "x" + width.toString());


    Offset tl = Offset(tl_x * pHeight/height, tl_y * pWidth/width);
    Offset tr = Offset(tr_x * pHeight/height, tr_y *  pWidth/width);
    Offset br = Offset(br_x * pHeight/height, br_y *  pWidth/width);
    Offset bl = Offset(bl_x * pHeight/height, bl_y *  pWidth/width);
    // int x = 20;
    // Offset tl = Offset((tl_y-x) * pWidth/width,(tl_x-x) * pHeight/height);
    // Offset tr = Offset((tr_y+x) *  pWidth/width  ,(tr_x -x)* pHeight/height) ;
    // Offset br = Offset((br_y+x) *  pWidth/width, (br_x+x) * pHeight/height);
    // Offset bl = Offset((bl_y+x) *  pWidth/width,(bl_x-x) * pHeight/height);

    canvas.drawLine(tl, tr, paint);
    canvas.drawLine(tr, br, paint);
    canvas.drawLine(br, bl, paint);
    canvas.drawLine(bl, tl, paint);

    // Offset one = Offset(0, 0);
    // Offset two = Offset(360, 0);
    // Offset three = Offset(360, 480);
    // Offset four = Offset(0, 480);
    // canvas.drawLine(one, two, paint);
    // canvas.drawLine(two, three, paint);
    // canvas.drawLine(three, four, paint);
    // canvas.drawLine(four, one, paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return true;
  }
}
