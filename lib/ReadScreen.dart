// import 'package:audioplayers/audioplayers.dart';
//import 'package:audioplayers/audioplayers_web.dart';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:just_audio/just_audio.dart';
import 'package:uni_voice/main.dart';
// import 'package:path_provider/path_provider.dart';

import 'model/listScan.dart';

class ReadScreen extends StatefulWidget {
  final Scan scan;

  const ReadScreen({Key key, @required this.scan}) : super(key: key);

  @override
  _ReadScreenState createState() => _ReadScreenState();
}

class _ReadScreenState extends State<ReadScreen> {
  int length = 0;
  int readPos = 0;
  bool isPause = false;
  final player = AudioPlayer();
  Duration duration;
  int index = 0;

  // AudioPlayer audioPlayer = AudioPlayer(playerId: "3");

  final list = [];
  @override
  initState() {
    super.initState();
    length = widget.scan.content.split("。").length;
    print(list.length);
    play();
  }

  dispose() {
    super.dispose();
    player.dispose();
    setState(() {
      isPause = true;
    });
  }

  Future play() async {
    await player.setAudioSource(
      ConcatenatingAudioSource(
        // Start loading next item just before reaching it.
        useLazyPreparation: true, // default
        // Customise the shuffle algorithm.
        // Specify the items in the playlist.
        children: [
          for (var i = 0; i < length; i++) ...[
            AudioSource.uri(Uri.parse(
                "/storage/emulated/0/Android/data/com.example.uni_voice/files/audio${widget.scan.pos}_$i.wav")),
          ]
        ],
      ),

      initialIndex: 0, // default

      initialPosition: Duration.zero, // default
    );
    await player.play();
  }

  // Future read(int i) async {
  //   await audioPlayer.play(
  //       "/storage/emulated/0/Android/data/com.example.uni_voice/files/audio${widget.scan.pos}_$i.wav",
  //       isLocal: true);
  //   readPos = i;
  //   //print("aaaa" + du.toString());
  //   //Future.delayed(Duration(milliseconds: 500));

  //   //audioPlayer = new AudioPlayer();
  // }

  // Future readAll(int i) async {
  //   print(i);
  //   if (i < length) {
  //     await audioPlayer
  //         .play(
  //             "/storage/emulated/0/Android/data/com.example.uni_voice/files/audio${widget.scan.pos}_$i.wav",
  //             isLocal: true)
  //         .whenComplete(() {
  //       Future.delayed(Duration(seconds: 4), () {
  //         i = i + 1;
  //         readAll(i);
  //       });
  //     });
  //   }
  // }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: new IconButton(
          icon: new Icon(Icons.arrow_back, color: Colors.black),
          onPressed: () {

           Navigator.pushReplacement(context, MaterialPageRoute(builder: (context) => MyHomePage(),));
          _stop();}

        ),
        centerTitle: true,
        title: Text(
          "Read Screen",
          style: TextStyle(color: Colors.black),
        ),
        backgroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(8, 0, 8, 0),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              _buildButtonPlay(Colors.green, Colors.greenAccent,
                  Icons.play_arrow, '', _pause),
              _buildButtonColumn(
                  Colors.red, Colors.redAccent, Icons.stop, '', _stop),
              _buildButtonColumn(
                  Colors.blue, Colors.blueAccent, Icons.circle, '', _play),
              _buildButtonColumn(Colors.blue, Colors.blueAccent,
                  Icons.skip_previous, "", _previous),
              _buildButtonColumn(
                  Colors.blue, Colors.blueAccent, Icons.skip_next, "", _next),
            ]),
          ),
          Container(
            margin: const EdgeInsets.fromLTRB(8, 0, 8, 0),
            color: Colors.amber.shade50,
            width: MediaQuery.of(context).size.width,
            height: MediaQuery.of(context).size.height,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(widget.scan.content,
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20)),
            ),
          )
        ]),
      ),
    );
  }

  Column _buildButtonColumn(Color color, Color splashColor, IconData icon,
      String label, Function func) {
    return Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
              icon: Icon(icon),
              color: color,
              splashColor: splashColor,
              onPressed: () => func()),
          Container(
              margin: const EdgeInsets.only(top: 8.0),
              child: Text(label,
                  style: TextStyle(
                      fontSize: 12.0,
                      fontWeight: FontWeight.w400,
                      color: color)))
        ]);
  }

  Column _buildButtonPlay(Color color, Color splashColor, IconData icon,
      String label, Function func) {
    return Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
              icon: isPause ? Icon(icon) : Icon(Icons.pause),
              color: color,
              splashColor: splashColor,
              onPressed: () => func()),
          Container(
              margin: const EdgeInsets.only(top: 8.0),
              child: Text(label,
                  style: TextStyle(
                      fontSize: 12.0,
                      fontWeight: FontWeight.w400,
                      color: color)))
        ]);
  }

  _play() async {
    await player.seek(Duration.zero, index: 0);
  }

  _next() async {
    await player.seekToNext();
  }

  _previous() async {
    await player.seekToPrevious();
  }

  _stop() async {
    await player.stop();
    setState(() {
      isPause = true;
    });
  }

  _pause() async {
    if (isPause == false) {
      await player.pause();
      index = player.currentIndex;

      duration = player.position;
    } else {
      await player.seek(duration, index: index);
      player.play();
    }
    setState(() {
      isPause = !isPause;
    });
  }
}
