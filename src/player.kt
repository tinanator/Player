package GUISamples



//import scala.collection.parallel.`ThreadPoolTasks$class`.queue

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.media.*
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.Stage
import java.io.File
import java.sql.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.floor

import org.apache.http.NameValuePair
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

var conn : Connection? = null

class sql {

    fun create(fileName: String) {
        val url = "jdbc:sqlite:D:/java2/$fileName"
        val sql2 = ("CREATE TABLE IF NOT EXISTS music_library (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " name text NOT NULL,\n"
                + " artist text,\n"
                + " album text,\n"
                + " genre text,\n"
                + " year text,\n"
                + " duration text,\n"
                + " dir text\n"

                + ");")
        try {
            val conn = DriverManager.getConnection(url)
            val stmt: Statement = conn.createStatement()
          //  stmt.execute(sql)
            stmt.execute(sql2)
        } catch (e: SQLException) {
            println(e.message)
        }

    }

    fun connect(fileName: String): Connection? {
        val url = "jdbc:sqlite:D:/java2/$fileName"
        var conn: Connection? = null
        try {
            conn = DriverManager.getConnection(url)
        } catch (e: SQLException) {
            println(e.message)
        }
        return conn
    }

    fun insert(name: String?, artist: String?, album: String?, genre: String?, year: String?, duration: String?, dir: String?) : Int? {

        var sql = "INSERT INTO music_library(name, artist, album, genre, year, duration, dir) VALUES(?,?,?,?,?,?,?)"
       // val sql = "SELECT name FROM music_library"
        try {
            val pstmt: PreparedStatement? = conn?.prepareStatement(sql)
            pstmt?.setString(1, name)
            pstmt?.setString(2, artist)
            pstmt?.setString(3, album)
            pstmt?.setString(4, genre)
            pstmt?.setString(5, year)
            pstmt?.setString(6, duration)
            pstmt?.setString(7, dir)
            pstmt?.executeUpdate()
            //val rs = stmt?.executeQuery("SELECT * FROM music_library")
            val stmt: Statement? = conn?.createStatement()
            return stmt?.executeQuery("SELECT max(id) AS max_id FROM music_library")?.getInt("max_id")

        } catch (e: SQLException) {
            println(e.message)
            return -1
        }

    }

    fun delete(id : Int) {
        try{
            val sql = ("DELETE  FROM music_library WHERE id = ?")
            val pstmt: PreparedStatement? = conn?.prepareStatement(sql)
            pstmt?.setInt(1, id)
            pstmt?.executeUpdate()
        }
        catch(e: SQLException){
            println(e.message)
        }
    }

    fun update(id : Int, newArtist : String, newTrack : String) {
        val sql = ("UPDATE music_library SET name = ?, artist = ? WHERE music_library.id = ?")
        val pstmt: PreparedStatement? = conn?.prepareStatement(sql)
        pstmt?.setString(1, newArtist)
        pstmt?.setString(2, newTrack)
        pstmt?.setInt(3, id)
        val updatedItems = pstmt?.executeUpdate()
        println(updatedItems)
    }

    fun print(l : ObservableList<SongModel>, m: MutableList<Media>) {
        val stmt: Statement? = conn?.createStatement()
        val rs = stmt?.executeQuery("SELECT * FROM music_library")
        while (rs!!.next()) {
            val track = SongModel(rs.getInt("Id"), rs.getString("artist"),
                    rs.getString("duration"),  rs.getString("name"),
                    rs.getString("album"), rs.getString("genre"),
                    rs.getString("year"), rs.getString("dir"))
            m.add(m.count(), Media(rs.getString("dir")))
            l.add(track)
        }

    }
}

class SongModel(id: Int,artist: String, duration: String, title: String, album: String, genre: String, year: String, dir: String) {
    var id : Int = 0
    var artist : String? = "UNKNOWN"
    var duration : String? = "UNKNOWN"
    var title : String? = "UNKNOWN"
    var album : String? = "UNKNOWN"
    var genre : String? = "UNKNOWN"
    var year : String? = "UNKNOWN"
    var dir : String? = "UNKNOWN"

    init {
        this.id = id
        if (artist!="null")  this.artist = artist
        if (title!="null") this.title = title
        if (album!="null") this.album = album
        if (genre!="null") this.genre = genre
        if (dir!="null") this.dir = dir
        if (duration!="null") this.duration = duration
        if (year!="null") this.year = year
    }
}



class MPlayer : Application() {


    var musicList = mutableListOf<Media>()
    var playList = mutableListOf<Media>()
    var index = 0
    var curMusic = -1
    var selectedMusic = -1
    internal var selectedFile: File? = null
    internal var mplayer2: MediaPlayer? = null
    private var songData = FXCollections.observableArrayList<SongModel>()
    private var playListData = FXCollections.observableArrayList<SongModel>()
    private var libraryTable = object : TableView<SongModel>(songData) {
        init {
            val column1 = TableColumn<SongModel, String>("title")
            val column2 = TableColumn<SongModel, String>("artist")
            val column3 = TableColumn<SongModel, String>("duration")
            val column4 = TableColumn<SongModel, String>("album")
            val column5 = TableColumn<SongModel, String>("genre")
            val column6 = TableColumn<SongModel, String>("year")
            val column7 = TableColumn<SongModel, String>("dir")
            column1.cellValueFactory = PropertyValueFactory<SongModel, String>("title")
            column2.cellValueFactory = PropertyValueFactory<SongModel, String>("artist")
            column3.cellValueFactory = PropertyValueFactory<SongModel, String>("duration")
            column4.cellValueFactory = PropertyValueFactory<SongModel, String>("album")
            column5.cellValueFactory = PropertyValueFactory<SongModel, String>("genre")
            column6.cellValueFactory = PropertyValueFactory<SongModel, String>("year")
            column7.cellValueFactory = PropertyValueFactory<SongModel, String>("dir")
            columns.addAll(column1, column2, column3)
        }
    }


    private var playListTable = object : TableView<SongModel>(playListData) {
        init {
            val column1 = TableColumn<SongModel, String>("title")
            val column2 = TableColumn<SongModel, String>("artist")
            val column3 = TableColumn<SongModel, String>("duration")
            val column4 = TableColumn<SongModel, String>("album")
            val column5 = TableColumn<SongModel, String>("genre")
            val column6 = TableColumn<SongModel, String>("year")
            val column7 = TableColumn<SongModel, String>("dir")
            column1.cellValueFactory = PropertyValueFactory<SongModel, String>("title")
            column2.cellValueFactory = PropertyValueFactory<SongModel, String>("artist")
            column3.cellValueFactory = PropertyValueFactory<SongModel, String>("duration")
            column4.cellValueFactory = PropertyValueFactory<SongModel, String>("album")
            column5.cellValueFactory = PropertyValueFactory<SongModel, String>("genre")
            column6.cellValueFactory = PropertyValueFactory<SongModel, String>("year")
            column7.cellValueFactory = PropertyValueFactory<SongModel, String>("dir")
            columns.addAll(column1, column2, column3)
        }
    }

    val spectrumSliders = HBox()
    val spectrumSlidersList : MutableList<Slider> = mutableListOf()



    private fun setFocusOnTrack(index: Int, t:TableView<SongModel>) {
        t.requestFocus();
        t.selectionModel.select(index);
        t.focusModel.focus(index);
    }

    private fun playMusic(index: Int, isChanged : Boolean) {
        if (mplayer2 != null && mplayer2?.status?.equals(MediaPlayer.Status.PAUSED)!! && !isChanged) {
            mplayer2?.volume = volumeSlider.value / 100
            mplayer2?.play()
        }
        else if (index >= 0) {
            if (mplayer2 != null) {
                mplayer2?.stop()
            }
            media = playList[index]
            mplayer2 = MediaPlayer(media)
            val bands = mplayer2?.audioEqualizer?.bands
            mplayer2?.audioEqualizer?.isEnabled = true
            mplayer2?.audioSpectrumNumBands
            mplayer2?.play()
            mplayer2?.volume = volumeSlider.value / 100
            playListData[index].title;
            curMusicLabel.text = playListData[index].title + " - " + playListData[index].artist;

        }

        mplayer2?.audioSpectrumNumBands = 16

        mplayer2?.audioSpectrumListener = AudioSpectrumListener { d, d2, floats, floats2 ->
            for (i in 0..15) {
                spectrumSlidersList[i].value = floats[i].toDouble() + 60 ;
                println(spectrumSlidersList[i].value.toString() + " / " + spectrumSlidersList[i].max.toString() + " from " + floats[i].toString())
            }
        }

        mplayer2?.currentTimeProperty()?.addListener(ChangeListener { obs, old, new ->
            val currentTime = new.toSeconds()
            val allTime = mplayer2?.stopTime!!.toSeconds()
            musicSlider.value = currentTime * 100.0 / allTime
            println("Cur time " + currentTime * 100.0 / allTime)
            val minutes: Double = floor(mplayer2?.currentTime!!.toMinutes())
            val seconds: Double = floor(mplayer2?.currentTime!!.toSeconds() % 60.0)
            val time: String = '0' + (minutes.toInt()).toString() + (seconds.toInt()).toString()
            val formatter = DateTimeFormatter.ofPattern("mm ss")
            val text = String.format("%02d : %02d", minutes.toInt(), seconds.toInt())
            setCurTime(text)

        })
    }


    private fun setCurTime(s: String) {
        curTrackTime.text = s
    }
    var media: Media? = null
    internal var musicSlider: Slider = Slider()
    val volumeSlider: Slider = Slider()
    var curMusicLabel: Label = Label("UNKNOWN")
    var curTrackTime: Label = Label("00 : 00")
    @Throws(Exception::class)

    fun getArtistInfo (artist : String) : String {
        var newstr = ""
        for (c in artist) {
            if (c != ' ') {
                newstr += c
            } else {
                newstr += "%20"
            }
        }

        val query = URIBuilder("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=$newstr&api_key=268dbf5a7309a8615ee5b2a5a2129354&format=json")
        val client = HttpClients.createDefault()
        val request = HttpGet(query.build())
        val response = client.execute(request)
        val entity = response.entity
        val response_content = EntityUtils.toString(entity)
        EntityUtils.consume(entity)
        return response_content
    }

    private fun trackTextFormatter(str : String) : String{
        var n = str.replace("(Sefon.me)", "")
        n = n.trim()
        n = n.toLowerCase().replace(" ", "")
        return n
    }

    fun getLyrics (artist:String, track:String, web : WebEngine) {
        val newStrArtist = trackTextFormatter(artist)
        val newStrTrack = trackTextFormatter(track)
        web.load("https://www.azlyrics.com/lyrics/$newStrArtist/$newStrTrack.html")
        //web.load("https://en.lyrsense.com/poets_of_the_fall/where_we_draw_the_line")
        //println("https://www.amalgama-lab.com/songs/" + newStrArtist[0] + "/" + newStrArtist + "/" + newStrTrack + ".html")
    }

    override fun start(primaryStage: Stage) {
        val bands = FXCollections.observableArrayList<EqualizerBand>()
        bands.addAll(EqualizerBand(32.0, 19.0, 0.0),
                EqualizerBand(64.0, 39.0, 0.0),
                EqualizerBand(125.0, 78.0, 0.0),
                EqualizerBand(250.0, 156.0, 0.0),
                EqualizerBand(500.0, 312.0, 0.0),
                EqualizerBand(1000.0, 625.0, 0.0),
                EqualizerBand(2000.0, 1250.0, 0.0),
                EqualizerBand(4000.0, 2500.0, 0.0),
                EqualizerBand(8000.0, 5000.0, 0.0),
                EqualizerBand(16000.0, 10000.0, 0.0))

        for (i in 0..15) {
            val eqSlider = Slider(0.0, 60.0, 0.0)
            eqSlider.maxHeight = 100.0
            eqSlider.orientation = Orientation.VERTICAL
            spectrumSliders.children.add(eqSlider)
            spectrumSlidersList.add(eqSlider)
        }

        val equalizerSliders = HBox()
        for (i in 0..9) {
            val eb = bands[i]
            val eqSlider = Slider(EqualizerBand.MIN_GAIN, EqualizerBand.MAX_GAIN, 0.0)
            eqSlider.minWidth = 30.0
            eqSlider.maxHeight = 100.0
            eqSlider.orientation = Orientation.VERTICAL
            equalizerSliders.children.add(eqSlider)
            if (volumeSlider.isPressed) {
                mplayer2?.volume = volumeSlider.value / 100
            }
            eqSlider.valueProperty().addListener(InvalidationListener{
                if (eqSlider.isPressed) {
                    mplayer2?.audioEqualizer?.bands?.get(i)?.gain = eqSlider.value
                }
            })
        }


        val tabs = TabPane()
        val playListTab = Tab("PlayList")
        val libraryTab = Tab("Library")
        val equalizerTab = Tab("Equalizer")
        val infoArtist = Tab("Info about artist")
        val lyricsTab = Tab("Lyrics")

        val removeBtn = Button("Remove")
        val addBtn = Button("Add to playlist")
        val vbox = VBox()

        val artistInput = TextField()
        val trackInput = TextField()
        val editBtn = Button("Edit")

        vbox.children.add(HBox(removeBtn, addBtn))
        vbox.children.add(HBox(trackInput, artistInput, editBtn))

        vbox.children.add(libraryTable)
        libraryTab.content = vbox
        playListTab.content = playListTable
        equalizerTab.content = VBox(equalizerSliders, spectrumSliders)

        val info = TextArea("EMPTY")
        info.isWrapText = true
        infoArtist.content = info

        val webView = WebView()
        val webEngine = webView.engine

        lyricsTab.content = webView

        editBtn.setOnAction { e ->
            if (libraryTable.selectionModel.focusedIndex >= 0 && artistInput.text.isNotEmpty() && trackInput.text.isNotEmpty()) {
                songData[libraryTable.selectionModel.focusedIndex].artist = artistInput.text
                songData[libraryTable.selectionModel.focusedIndex].title = trackInput.text
                libraryTable.refresh()
                sql().update(songData[libraryTable.selectionModel.focusedIndex].id, trackInput.text, artistInput.text)
                artistInput.text = ""
                trackInput.text = ""
            }
        }

        addBtn.setOnAction {e ->
            if (libraryTable.focusModel.focusedIndex >= 0) {
                playListData.add(songData[libraryTable.focusModel.focusedIndex])
                playList.add(musicList[libraryTable.focusModel.focusedIndex])
                if (playListData.count() == 1) {
                    setFocusOnTrack(0, playListTable)
                }
            }
        }
        removeBtn.setOnAction {e ->
            if (songData.count() > 0) {
                songData[libraryTable.focusModel.focusedIndex].id.let { sql().delete(it) }
                index = libraryTable.focusModel.focusedIndex
                songData.removeAt(index)
                musicList.removeAt(index)
            }
        }

        tabs.tabs.addAll(playListTab, libraryTab, equalizerTab, infoArtist, lyricsTab)

        val root = object : BorderPane() {
            init {
                val filenameLabel = Label("")
                val fileChooser = FileChooser()
                fileChooser.title = "Open File"
                fileChooser.extensionFilters.addAll(
                        ExtensionFilter("Audio Files", "*.wav", "*.mp3")
                )
                val vbox = object : VBox() {
                    init {
                        children.add(filenameLabel)
                        val hbox = object : HBox() {
                            init {
                                val playButton = Button("Play")
                                val pauseButton = Button("Pause")
                                val nextButton = Button("next")
                                val prevButton = Button("prev")

                                playButton.setOnAction { e ->
                                    playMusic(playListTable.selectionModel.focusedIndex, false)
                                    curMusic = playListTable.selectionModel.focusedIndex
                                    if (curMusic != -1) {
                                        val artistInfoJson = playListData[curMusic].artist?.let { getArtistInfo(it) }
                                        val gson = Gson()
                                        val tmp = gson.fromJson(artistInfoJson, JsonObject::class.java)["artist"]
                                        val t = gson.fromJson(tmp, JsonObject::class.java)["bio"]
                                        val t2 = gson.fromJson(t, JsonObject::class.java)["content"]
                                        val content = gson.fromJson(t2, String::class.java)
                                        info.text = content

                                        playListData[curMusic].artist?.let { playListData[curMusic].title?.let { it1 -> getLyrics(it, it1, webEngine) } }
                                    }

                                }
                                pauseButton.setOnAction { e -> mplayer2?.pause() }
                                nextButton.setOnAction { e ->
                                    if (curMusic > -1) {
                                        curMusic = (curMusic + 1) % playList.count()
                                        setFocusOnTrack(curMusic, playListTable)
                                        playMusic(curMusic, true)
                                        val artistInfoJson = playListData[curMusic].artist?.let { getArtistInfo(it) }
                                        val gson = Gson()
                                        val tmp = gson.fromJson(artistInfoJson, JsonObject::class.java)["artist"]
                                        val t = gson.fromJson(tmp, JsonObject::class.java)["bio"]
                                        val t2 = gson.fromJson(t, JsonObject::class.java)["content"]
                                        val content = gson.fromJson(t2, String::class.java)
                                        info.text = content
                                        playListData[curMusic].artist?.let { playListData[curMusic].title?.let { it1 -> getLyrics(it, it1, webEngine) } }

                                    }
                                }
                                prevButton.setOnAction { e ->
                                    if (curMusic > -1) {
                                        curMusic = (curMusic + playList.count() - 1) % playList.count()
                                        setFocusOnTrack(curMusic, playListTable)
                                        playMusic(curMusic, true)
                                        val artistInfoJson = playListData[curMusic].artist?.let { getArtistInfo(it) }
                                        val gson = Gson()
                                        val tmp = gson.fromJson(artistInfoJson, JsonObject::class.java)["artist"]
                                        val t = gson.fromJson(tmp, JsonObject::class.java)["bio"]
                                        val t2 = gson.fromJson(t, JsonObject::class.java)["content"]
                                        val content = gson.fromJson(t2, String::class.java)
                                        info.text = content
                                        playListData[curMusic].artist?.let { playListData[curMusic].title?.let { it1 -> getLyrics(it, it1, webEngine) } }

                                    }
                                }
                                val stopButton = object : Button("Stop") {
                                    init {
                                        setOnAction {e -> mplayer2?.stop() }
                                    }
                                }
                                volumeSlider.prefWidth = 100.0
                                volumeSlider.minWidth = 100.0
                                volumeSlider.value = 100.0
                                children.addAll(playButton, pauseButton, stopButton, nextButton, prevButton, volumeSlider)
                            }
                        }
                        children.addAll(hbox, curMusicLabel, musicSlider, curTrackTime)
                    }
                }

                center = vbox

                val menubar = object : MenuBar() {
                    init {
                        val menu = object : Menu("File") {
                            init {
                                val selectMenuItem = object : MenuItem("Select") {
                                    init {
                                        setOnAction { e ->
                                            val selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage)
                                            if (selectedFiles != null) {
                                                var q : Queue<Media> = LinkedList<Media>()
                                                for (file in selectedFiles) {
                                                    if (file != null) {
                                                        // mplayer2?.stop();
                                                        val url = file.toURI()

                                                        println(url.toString())
                                                        val media = Media(url.toString())
                                                        val mp = MediaPlayer(media)
                                                        mp.setOnReady {->
                                                            val duration = mp.media.duration
                                                            val minutes:Double = floor(duration.toMinutes())
                                                            val seconds = duration?.toSeconds()?.rem(60.0)?.let { floor(it) }
                                                            val id = sql().insert(mp.media.metadata["title"].toString(),
                                                                    mp.media.metadata["artist"].toString(), mp.media.metadata["album"].toString(),
                                                                    mp.media.metadata["genre"].toString(), mp.media.metadata["year"].toString(),
                                                                    minutes.toLong().toString() + ':' + seconds?.toLong().toString(),
                                                                    mp.media.source)
                                                            val track = SongModel(id!!, mp.media.metadata["artist"].toString(), minutes.toLong().toString() + ':' + seconds?.toLong().toString(),
                                                                    mp.media.metadata["title"].toString(), mp.media.metadata["album"].toString(),    mp.media.metadata["genre"].toString(),
                                                                    mp.media.metadata["year"].toString(), mp.media.source)


                                                            songData.add(track)
                                                            musicList.add(musicList.count(), mp.media)
                                                            musicSlider.min = 0.0
                                                            musicSlider.max = 100.0

                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }
                                val pauseMenuItem = MenuItem("Pause")
                                val playMenuItem = MenuItem("Play")
                                val stopMenuItem = MenuItem("Stop")

                                items.addAll(selectMenuItem, playMenuItem, pauseMenuItem, stopMenuItem)
                            }
                        }
                        menus.add(menu)
                    }
                }
                top = menubar
                libraryTable.prefHeight = 1000.0
                bottom = tabs
            }
        }
        playListTable.setOnMousePressed { mouseEvent ->
           playListTable.isFocusTraversable = false;
            if (mouseEvent.clickCount == 2) {
          //      mplayer2?.stop()
                curMusic = playListTable.selectionModel.focusedIndex
                if (mplayer2 == null) {
                    mplayer2 = MediaPlayer(playList[curMusic])
                }
                playMusic(curMusic, true)
                setFocusOnTrack(curMusic, playListTable)
                val artistInfoJson = playListData[curMusic].artist?.let { getArtistInfo(it) }
                val gson = Gson()
                val tmp = gson.fromJson(artistInfoJson, JsonObject::class.java)["artist"]
                val t = gson.fromJson(tmp, JsonObject::class.java)["bio"]
                val t2 = gson.fromJson(t, JsonObject::class.java)["content"]
                val content = gson.fromJson(t2, String::class.java)
                info.text = content
                playListData[curMusic].artist?.let { playListData[curMusic].title?.let { it1 -> getLyrics(it, it1, webEngine) } }

            }
        }

        sql().print(songData, musicList)
        if (songData.count() > 0) {
            curMusic = 0
            //setFocusOnTrack(0, libraryTable)
        }


        musicSlider.valueProperty().addListener(InvalidationListener {
            if (musicSlider.isPressed) {
                var newTime = musicSlider.value;
                val duration = mplayer2?.media?.duration;
                mplayer2?.seek(duration?.multiply(musicSlider.value / 100.0));
            }
        })
        volumeSlider.valueProperty().addListener(InvalidationListener {
            if (volumeSlider.isPressed) {
                mplayer2?.volume = volumeSlider.value / 100 // It would set the volume
            }
        })
        val scene = Scene(root, 400.0, 500.0)
        primaryStage.scene = scene
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
        //    sql().connect()
            conn = sql().connect("db.db")
            sql().create("db.db")

            launch(MPlayer::class.java)
        }
    }

}

