package main;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * A simple app to download any Pinterest user's pins to a local directory.
 */
@SuppressWarnings("ALL")
public class Main {

    ///<editor-fold desc="Properties">
    private static final int TIMEOUT = 10000;
    private static String username;
    private static String rootDir = "";
    private static JSONObject userObj = null;
    private static JSONArray boardsArr = null;
    private static JSONArray bookmarks = null;
    private static String boardName;

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        Main.username = username;
    }

    public static String getRootDir() {
        return rootDir;
    }

    public static void setRootDir(String rootDir) {
        Main.rootDir = rootDir;
    }

    ///</editor-fold>

    /**
     * Verify arguments, and handle some errors
     *
     * @param args arguments (needs a string for username or abort)
     */
    public static void main(final String[] args)  {
        System.out.println("Welcome to PinSaver!");

        extractUsername(args);// get username

        try {
            // validate username and connect to their page
            System.out.println("\nLoading...");
            Connection.Response doc;
            try {
                doc = getUserDoc();
            } catch (HttpStatusException e) {
                System.out.println("ERROR: not a valid user name, aborting.");
                return;
            }

            userObj = new JSONObject(doc.body());
            extractBoardsArr(doc);
            createDirForPictures();
            showAlbums();
            processUsersChoice();

            System.out.println("Thanks for using PinSaver!");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void extractUsername(String []args) {
        if (args.length > 0) {
            username = args[0];
        } else {
            System.out.println("Please enter your username:");
            Scanner s = new Scanner(System.in);
            username = s.next();
            if (username.length() == 0) {
                System.out.println("ERROR: please enter a user name, aborting.");
                return;
            }
        }

        username = FileStreamHelper.cleanFilename(username.trim());
        if (username.contains(" ")) {
            System.out.println("ERROR: username contains space character");
            return;
        }
    }

    private static void processUsersChoice() throws JSONException{
        System.out.print("\nEnter the number of album: ");
        Scanner s = new Scanner(System.in);

        int choice = 0;
        try {
            choice = Integer.parseInt(s.next());
        } catch (Exception e) {
            System.out.println("Wrong number, using 0: All albums");
        }
        if (choice > 0) {
            downloadAlbum(boardsArr.getJSONObject(choice-1));
        } else
            for (int boardNumber=0; boardNumber<boardsArr.length(); boardNumber++) {
                downloadAlbum((JSONObject) boardsArr.get(boardNumber));
            }

        System.out.println("All pins were downloaded to " + System.getProperty("user.dir")
                + File.separator  + rootDir + File.separator);
    }

    private static void showAlbums() throws JSONException{
        System.out.println("\nAvailable albums:");

        for (int boardNumber=0; boardNumber<boardsArr.length(); boardNumber++) {
            JSONObject boardObj = (JSONObject)boardsArr.get(boardNumber);
            System.out.println(
                    Integer.toString(boardNumber+1)
                            +". "+boardObj.getString("name")
                            +" ("+boardObj.getInt("pin_count")+" pics)"
            );
        }
        System.out.println("0. ALL ALBUMS");
    }

    private static void createDirForPictures() {
        rootDir += username;
        String sdf = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        rootDir += "_" + sdf;
        if(!FileStreamHelper.makeDir(rootDir))
            return;
        System.out.println("\nPins will be downloaded to " + rootDir);
    }

    private static Connection.Response getUserDoc() throws IOException{
        String line = "{\"options\":{\"username\":\"" + username + "\",\"page_size\":250},\"module\":{\"name\":\"UserProfileContent\",\"options\":{\"tab\":\"boards\"}}}";
        return Jsoup.connect("https://www.pinterest.com/resource/UserResource/get/")
                .data("data", line)
                .header("X-Requested-With", "XMLHttpRequest")
                .ignoreContentType(true)
                .maxBodySize(0)
                .timeout(TIMEOUT).execute();
    }

    private static void extractBoardsArr(Connection.Response doc) {
        try {
            doc = Jsoup.connect("https://www.pinterest.com/resource/BoardsResource/get/")
                    .data("data",
                            "{\"options\":{\"username\":\"" + username + "\",\"field_set_key\":\"grid_item\"}}")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .timeout(TIMEOUT).execute();
            userObj = new JSONObject(doc.body());
            boardsArr = userObj.getJSONObject("resource_response").getJSONArray("data");
        } catch (Exception e6) {
            System.out.println("ERROR: plan B failed. Try again later");
            return;
        }
    }

    private static void downloadAlbum(JSONObject boardObj) throws JSONException{
        extractBoardName(boardObj);
        System.out.println("Downloading '" + boardName + "'...");

        bookmarks = new JSONArray("[\"\"]");
        while (!bookmarks.getString(0).equals("-end-")) {
            Connection.Response boardDoc = getBoardDoc(boardObj);
            if (boardDoc != null) {
                saveImagesFromBoard(boardDoc);
            } else
                System.out.println("Board is empty!");
        }
    }

    private static void extractBoardName(JSONObject boardObj) throws JSONException{
        boardName = boardObj.getString("name");
        if(boardName == null || boardName.isEmpty()) {
            System.out.println("ERROR: couldn't find name of board, it's the developer's fault. Aborting.");
            return;
        }
        boardName = FileStreamHelper.cleanFilename(boardName);
        if(!FileStreamHelper.makeDir(rootDir + File.separator + boardName))
            return;
    }

    public static Connection.Response getBoardDoc(JSONObject boardObj) throws JSONException{
        try {
            return Jsoup.connect("https://www.pinterest.com/resource/BoardFeedResource/get/")
                .data("data", "{\"options\":{\"board_id\":\"" + boardObj.getString("id") + "\",\"page_size\":250,\"bookmarks\":" + bookmarks.toString() + "}}")
                .header("X-Requested-With", "XMLHttpRequest")
                .ignoreContentType(true)
                .maxBodySize(0)
                .timeout(TIMEOUT).execute();
        } catch (IOException e) {
            System.out.println("Error downloading board!");
            e.printStackTrace();
            return null;
        }
    }

    public static void saveImagesFromBoard(Connection.Response boardDoc) throws JSONException{
        JSONObject obj = new JSONObject(boardDoc.body());
        JSONArray arr = obj.getJSONObject("resource_response").getJSONArray("data");
        bookmarks = obj.getJSONObject("resource").getJSONObject("options").getJSONArray("bookmarks");

        for (int i = 0; i < arr.length()-1; i++) {
            String url = arr.getJSONObject(i).getJSONObject("images").getJSONObject("orig").getString("url");
            String path = rootDir + File.separator + boardName;
            String fileName = i + "_" + arr.getJSONObject(i).getString("description");
            FileStreamHelper.saveImage(url, path,  fileName);
        }
    }
}