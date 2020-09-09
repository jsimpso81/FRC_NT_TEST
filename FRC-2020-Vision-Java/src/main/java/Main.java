/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

import org.opencv.core.Mat;

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main {
  private static String configFile = "/boot/frc.json";

  @SuppressWarnings("MemberName")
  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
  }

  @SuppressWarnings("MemberName")
  public static class SwitchedCameraConfig {
    public String name;
    public String key;
  };

  public static int team;
  public static boolean server;
  public static List<CameraConfig> cameraConfigs = new ArrayList<>();
  public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();
  public static List<VideoSource> cameras = new ArrayList<>();


  private static double wDog;
  private static double ntTest0;
  private static double ntTest1;
  private static double ntTest2;
  private static double ntTest3;
  private static int ntTest0INT;
  private static int ntTest1INT;
  private static int ntTest2INT;
  private static int ntTest3INT;

//  private static cls4150Timer objNoTargetDelayTimer = new cls4150Timer( 0.020D );

//  private static double dblLastGoodTargetAngle = 0.0D; 
//  private static double dblLastGoodRobotAngle = 0.0D;

private static clsNetTblEntryInfo objNT_Test0            = new clsNetTblEntryInfo("/SmartDashboard/Test0");
private static clsNetTblEntryInfo objNT_Test1            = new clsNetTblEntryInfo("/SmartDashboard/Test1");
private static clsNetTblEntryInfo objNT_Test2            = new clsNetTblEntryInfo("/SmartDashboard/Test2");
private static clsNetTblEntryInfo objNT_Test3            = new clsNetTblEntryInfo("/SmartDashboard/Test3");
private static clsNetTblEntryInfo objNT_Test0FB           = new clsNetTblEntryInfo("/SmartDashboard/Test0FB");
private static clsNetTblEntryInfo objNT_Test1FB            = new clsNetTblEntryInfo("/SmartDashboard/Test1FB");
private static clsNetTblEntryInfo objNT_Test2FB            = new clsNetTblEntryInfo("/SmartDashboard/Test2FB");
private static clsNetTblEntryInfo objNT_Test3FB            = new clsNetTblEntryInfo("/SmartDashboard/Test3FB");

  private static clsNetTblEntryInfo objVisRobotAngle            = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/angle");

  private static clsNetTblEntryInfo objVisTargRobotAngleOut     = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/RobotAngleFeedback");
  private static clsNetTblEntryInfo objVisTargAngle             = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/TargetAngle");
  private static clsNetTblEntryInfo objVisTargFound             = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/TargetFound");
  private static clsNetTblEntryInfo objVisTargWatchDog          = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/TargetWatchDog");
  private static clsNetTblEntryInfo objVisTargSelectedScore     = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/SelectedScore");
  private static clsNetTblEntryInfo objVisTargHighestScore      = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/HighestScore");
  private static clsNetTblEntryInfo objVisTargTapeCount         = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/TapeCount");
  private static clsNetTblEntryInfo objVisTargTargetCount       = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/TargetCount");
  private static clsNetTblEntryInfo objVisTargTargetCombos      = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/TargetCombos");

  private static clsNetTblEntryInfo objVisDebugFoundTarg          = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/ReallyFoundTarg");
  private static clsNetTblEntryInfo objVisDebugIndex              = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/TargetIndex");
  private static clsNetTblEntryInfo objVisDebugWidth              = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/Width");
  private static clsNetTblEntryInfo objVisDebugHeight             = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/Height");
  private static clsNetTblEntryInfo objVisDebugLeftIndex          = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/LeftIndex");
  private static clsNetTblEntryInfo objVisDebugRightIndex         = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/RightIndex");
  private static clsNetTblEntryInfo objVisDebugLeftAngle          = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/LeftAngle");
  private static clsNetTblEntryInfo objVisDebugRightAngle         = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/RightAngle");
  private static clsNetTblEntryInfo objVisDebugLeftScore          = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/LeftScore");
  private static clsNetTblEntryInfo objVisDebugRightScore         = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/RightScore");
  private static clsNetTblEntryInfo objVisDebugLeftRatio          = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/LeftRatio");
  private static clsNetTblEntryInfo objVisDebugRightRatio         = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/RightRatio");
  private static clsNetTblEntryInfo objVisDebugHeightRatioScore   = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/HeightRatioScore");
  private static clsNetTblEntryInfo objVisDebugWidthRatioScore    = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/WidthRatioScore");
  private static clsNetTblEntryInfo objVisDebugOverallRatioScore  = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/AreaRatioScore");
  private static clsNetTblEntryInfo objVisDebugHeightOffsetScore  = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/HeightOffsetScore");
  private static clsNetTblEntryInfo objVisDebugOverallScore       = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/OverallScore");
  private static clsNetTblEntryInfo objVisDebugCenterPixel        = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/CenterPixel");
  private static clsNetTblEntryInfo objVisDebugCenterOffsetPixel  = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/CenterOffsetPixel");
  private static clsNetTblEntryInfo objVisDebugCenterOffsetAngle  = new clsNetTblEntryInfo("/SmartDashboard/FrcVision/VisionTarg/Debug/CenterOffsetAngle");






  private Main() {
  }

  /**
   * Report parse error.
   */
  public static void parseError(String str) {
    System.err.println("config error in '" + configFile + "': " + str);
  }

  /**
   * Read single camera configuration.
   */
  public static boolean readCameraConfig(JsonObject config) {
    CameraConfig cam = new CameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement pathElement = config.get("path");
    if (pathElement == null) {
      parseError("camera '" + cam.name + "': could not read path");
      return false;
    }
    cam.path = pathElement.getAsString();

    // stream properties
    cam.streamConfig = config.get("stream");

    cam.config = config;

    cameraConfigs.add(cam);
    return true;
  }

  /**
   * Read single switched camera configuration.
   */
  public static boolean readSwitchedCameraConfig(JsonObject config) {
    SwitchedCameraConfig cam = new SwitchedCameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read switched camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement keyElement = config.get("key");
    if (keyElement == null) {
      parseError("switched camera '" + cam.name + "': could not read key");
      return false;
    }
    cam.key = keyElement.getAsString();

    switchedCameraConfigs.add(cam);
    return true;
  }

  /**
   * Read configuration file.
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public static boolean readConfig() {
    // parse file
    JsonElement top;
    try {
      top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
    } catch (IOException ex) {
      System.err.println("could not open '" + configFile + "': " + ex);
      return false;
    }

    // top level must be an object
    if (!top.isJsonObject()) {
      parseError("must be JSON object");
      return false;
    }
    JsonObject obj = top.getAsJsonObject();

    // team number
    JsonElement teamElement = obj.get("team");
    if (teamElement == null) {
      parseError("could not read team number");
      return false;
    }
    team = teamElement.getAsInt();

    // ntmode (optional)
    if (obj.has("ntmode")) {
      String str = obj.get("ntmode").getAsString();
      if ("client".equalsIgnoreCase(str)) {
        server = false;
      } else if ("server".equalsIgnoreCase(str)) {
        server = true;
      } else {
        parseError("could not understand ntmode value '" + str + "'");
      }
    }

    // cameras
    JsonElement camerasElement = obj.get("cameras");
    if (camerasElement == null) {
      parseError("could not read cameras");
      return false;
    }
    JsonArray cameras = camerasElement.getAsJsonArray();
    for (JsonElement camera : cameras) {
      if (!readCameraConfig(camera.getAsJsonObject())) {
        return false;
      }
    }

    if (obj.has("switched cameras")) {
      JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
      for (JsonElement camera : switchedCameras) {
        if (!readSwitchedCameraConfig(camera.getAsJsonObject())) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Start running the camera.
   */
  public static VideoSource startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }

  /**
   * Start running the switched camera.
   */
  public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config) {
    System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
    MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

    NetworkTableInstance.getDefault()
        .getEntry(config.key)
        .addListener(event -> {
              if (event.value.isDouble()) {
                int i = (int) event.value.getDouble();
                if (i >= 0 && i < cameras.size()) {
                  server.setSource(cameras.get(i));
                }
              } else if (event.value.isString()) {
                String str = event.value.getString();
                for (int i = 0; i < cameraConfigs.size(); i++) {
                  if (str.equals(cameraConfigs.get(i).name)) {
                    server.setSource(cameras.get(i));
                    break;
                  }
                }
              }
            },
            EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    return server;
  }

  /**
   * Example pipeline.
   */
  public static class MyPipeline implements VisionPipeline {
    public int val;

    @Override
    public void process(Mat mat) {
      val += 1;
    }
  }

  //=========================================================================================================
  /**
   * Main.
   */
  public static void main(String... args) {
    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!readConfig()) {
      return;
    }

    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
    }

    //--------init our networ table entries.
    objNT_Test0.SetTable( ntinst );
    objNT_Test1.SetTable( ntinst );
    objNT_Test2.SetTable( ntinst );
    objNT_Test3.SetTable( ntinst );
    objNT_Test0FB.SetTable( ntinst );
    objNT_Test1FB.SetTable( ntinst );
    objNT_Test2FB.SetTable( ntinst );
    objNT_Test3FB.SetTable( ntinst );
    objVisRobotAngle.SetTable( ntinst );

    objVisTargRobotAngleOut.SetTable( ntinst );
    objVisTargAngle.SetTable( ntinst );
    objVisTargFound.SetTable( ntinst );
    objVisTargWatchDog.SetTable( ntinst );
    objVisTargSelectedScore.SetTable( ntinst );
    objVisTargHighestScore.SetTable( ntinst );
    objVisTargTapeCount.SetTable( ntinst );
    objVisTargTargetCount.SetTable( ntinst );
    objVisTargTargetCombos.SetTable( ntinst );

    objVisDebugFoundTarg.SetTable( ntinst );
    objVisDebugIndex.SetTable( ntinst );
    objVisDebugWidth.SetTable( ntinst );
    objVisDebugHeight.SetTable( ntinst );
    objVisDebugLeftIndex.SetTable( ntinst );
    objVisDebugRightIndex.SetTable( ntinst );
    objVisDebugLeftAngle.SetTable( ntinst );
    objVisDebugRightAngle.SetTable( ntinst );
    objVisDebugLeftScore.SetTable( ntinst );
    objVisDebugRightScore.SetTable( ntinst );
    objVisDebugLeftRatio.SetTable( ntinst );
    objVisDebugRightRatio.SetTable( ntinst );
    objVisDebugHeightRatioScore.SetTable( ntinst );
    objVisDebugWidthRatioScore.SetTable( ntinst );
    objVisDebugOverallRatioScore.SetTable( ntinst );
    objVisDebugHeightOffsetScore.SetTable( ntinst );
    objVisDebugOverallScore.SetTable( ntinst );
    objVisDebugCenterPixel.SetTable( ntinst );
    objVisDebugCenterOffsetPixel.SetTable( ntinst );
    objVisDebugCenterOffsetAngle.SetTable( ntinst );
    // ======== end team 4150 code

    // start cameras
    for (CameraConfig config : cameraConfigs) {
      cameras.add(startCamera(config));
    }

    // start switched cameras
    for (SwitchedCameraConfig config : switchedCameraConfigs) {
      startSwitchedCamera(config);
    }

    // start image processing on camera 0 if present
    if (cameras.size() >= 1) {
      VisionThread visionThread = new VisionThread(cameras.get(0),
              new MyPipeline(), pipeline -> {
        // do something with pipeline results
      });
      /* something like this for GRIP:
      VisionThread visionThread = new VisionThread(cameras.get(0),
              new GripPipeline(), pipeline -> {
        ...
      });
       */
      visionThread.start();
    }

    // loop forever
    for (;;) {

      //--------Test0
      ntTest0 = objNT_Test0.GetDouble(0.0D);
      ntTest0INT = Math.abs((int)ntTest0);
      if ( ntTest0INT % 4 == 0 ) {
        ntTest0INT += 1;
        if ( ntTest0INT > 65535 ) {
          ntTest0INT = 0;
        }
        ntTest0 = (double)ntTest0INT;
        objNT_Test0.WriteDouble(ntTest0);
        ntTest0 = objNT_Test0.GetDouble(-2.0D);
        objNT_Test0FB.WriteDouble(ntTest0);
        //System.out.println("udpating Test0 =" + ntTest0  );
      }


      //--------Test1
      ntTest1 = objNT_Test1.GetDouble(0.0D);
      ntTest1INT = Math.abs((int)ntTest1);;
      if ( ntTest1INT % 4 == 1 ) {
        ntTest1INT += 1;
        if ( ntTest1INT > 65535 ) {
          ntTest1INT = 0;
        }
        ntTest1 = (double)ntTest1INT;
        objNT_Test1.WriteDouble(ntTest1);
        ntTest1 = objNT_Test1.GetDouble(-2.0D);
        objNT_Test1FB.WriteDouble(ntTest1);
      }

      //--------Test2
      ntTest2 = objNT_Test2.GetDouble(0.0D);
      ntTest2INT = Math.abs((int)ntTest2);
      if ( ntTest2INT % 4 == 2 ) {
        ntTest2INT += 1;
        if ( ntTest2INT > 65535 ) {
          ntTest2INT = 0;
        }
        ntTest2 = (double)ntTest2INT;
        objNT_Test2.WriteDouble(ntTest2);
        ntTest2 = objNT_Test2.GetDouble(-2.0D);
        objNT_Test2FB.WriteDouble(ntTest2);
      }

      //--------Test3
      ntTest3 = objNT_Test3.GetDouble(0.0D);
      ntTest3INT = Math.abs((int)ntTest3);
      if ( ntTest3INT % 4 == 3 ) {
        ntTest3INT += 1;
        if ( ntTest3INT > 65535 ) {
          ntTest3INT = 0;
        }
        ntTest3 = (double)ntTest3INT;
        objNT_Test3.WriteDouble(ntTest3);
        ntTest3 = objNT_Test3.GetDouble(-2.0D);
        objNT_Test3FB.WriteDouble(ntTest3);
      }

      objVisTargWatchDog.WriteDouble(wDog++);

      //--------slightly faster update to server
      ntinst.flush();

      try {
        Thread.sleep(20);
        
      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}
