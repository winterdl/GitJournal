import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:journal/serializers.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'package:uuid/uuid.dart';

import 'package:journal/note.dart';
import 'package:journal/file_storage.dart';

Future<Directory> getNotesDir() async {
  var appDir = await getApplicationDocumentsDirectory();
  var dir = new Directory(p.join(appDir.path, "notes"));
  await dir.create();

  return dir;
}

class StateContainer extends StatefulWidget {
  final Widget child;

  StateContainer({
    @required this.child,
  });

  static StateContainerState of(BuildContext context) {
    return (context.inheritFromWidgetOfExactType(_InheritedStateContainer)
            as _InheritedStateContainer)
        .data;
  }

  @override
  State<StatefulWidget> createState() {
    return StateContainerState();
  }
}

class StateContainerState extends State<StateContainer> {
  AppState appState = AppState.loading();
  FileStorage fileStorage;

  @override
  void initState() {
    super.initState();

    fileStorage = new FileStorage(
      getDirectory: getNotesDir,
      noteSerializer: new MarkdownYAMLSerializer(),
    );

    fileStorage.loadNotes().then((loadedNotes) {
      setState(() {
        appState = AppState(notes: loadedNotes);
      });
    }).catchError((err) {
      setState(() {
        print("Got Error");
        print(err);
        appState.isLoading = false;
      });
    });
  }

  @override
  void setState(VoidCallback fn) {
    super.setState(fn);

    fileStorage.saveNotes(appState.notes);
  }

  void addNote(Note note) {
    setState(() {
      note.id = new Uuid().v4();
      appState.notes.insert(0, note);
    });
  }

  void removeNote(Note note) {
    setState(() {
      appState.notes.remove(note);
    });
  }

  void insertNote(int index, Note note) {
    setState(() {
      appState.notes.insert(index, note);
    });
  }

  // FIXME: Implement this!
  void updateNote(Note note) {
    setState(() {
      //appState.notes.
      //appState.notes.remove(note);
    });
  }

  @override
  Widget build(BuildContext context) {
    return _InheritedStateContainer(
      data: this,
      child: widget.child,
    );
  }
}

class _InheritedStateContainer extends InheritedWidget {
  final StateContainerState data;

  _InheritedStateContainer({
    Key key,
    @required this.data,
    @required Widget child,
  }) : super(key: key, child: child);

  // Note: we could get fancy here and compare whether the old AppState is
  // different than the current AppState. However, since we know this is the
  // root Widget, when we make changes we also know we want to rebuild Widgets
  // that depend on the StateContainer.
  @override
  bool updateShouldNotify(_InheritedStateContainer old) => true;
}