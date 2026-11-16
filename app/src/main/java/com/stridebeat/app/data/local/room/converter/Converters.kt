package com.stridebeat.app.data.local.room.converter

import androidx.room.TypeConverter
import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromLatLngList(points: List<LatLng>): String {
        val array = JSONArray()
        points.forEach { p ->
            val obj = JSONObject()
            obj.put("lat", p.latitude)
            obj.put("lng", p.longitude)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toLatLngList(json: String?): List<LatLng> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<LatLng>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(LatLng(obj.getDouble("lat"), obj.getDouble("lng")))
        }
        return list
    }

    @TypeConverter
    fun fromPlayedTrackList(tracks: List<PlayedTrack>): String {
        val array = JSONArray()
        tracks.forEach { t ->
            val obj = JSONObject()
            obj.put("trackId", t.trackId)
            obj.put("title", t.title)
            obj.put("artist", t.artist)
            obj.put("source", t.source.name)
            obj.put("playedAt", t.playedAt)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toPlayedTrackList(json: String?): List<PlayedTrack> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<PlayedTrack>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val trackId = obj.getString("trackId")
            val sourceName = obj.optString("source", "SPOTIFY")
            val rawSource = try { MusicSource.valueOf(sourceName) } catch (_: Exception) { MusicSource.SPOTIFY }
            val remappedSource = com.stridebeat.app.util.TrackMatchingEngine.remapLegacySource(trackId, rawSource)
            list.add(
                PlayedTrack(
                    trackId = trackId,
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    source = remappedSource,
                    playedAt = obj.getLong("playedAt")
                )
            )
        }
        return list

    }
}
