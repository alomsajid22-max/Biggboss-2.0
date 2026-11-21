package com.example.multiplayer

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ArchiveBg = Color(0xFF0C1222)
private val ArchiveCard = Color(0xFF151E33)
private val ArchiveElevated = Color(0xFF1C2742)
private val ArchiveGold = Color(0xFFEAB954)
private val ArchiveCyan = Color(0xFF22D3EE)
private val ArchiveEmerald = Color(0xFF34D399)
private val ArchiveDanger = Color(0xFFF87171)
private val ArchiveMuted = Color(0xFF94A3B8)

object CctvPlaybackMath {
  val SUPPORTED_SPEEDS = listOf(0.5f, 1.0f, 1.5f, 2.0f)

  fun clampSeekMs(targetMs: Long, durationMs: Long): Long {
    val maxMs = durationMs.coerceAtLeast(0L)
    return targetMs.coerceIn(0L, maxMs)
  }

  fun rewind5s(currentMs: Long, durationMs: Long): Long {
    return clampSeekMs(currentMs - 5_000L, durationMs)
  }

  fun formatDurationMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
  }

  fun formatRecordedAt(millis: Long): String {
    if (millis <= 0L) return "Unknown time"
    return SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US).format(Date(millis))
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.2f MB", kb / 1024.0)
  }
}

@Composable
fun AdminCctvArchiveTab(
  recordings: List<CctvRecordingRecord>,
  isUploadingCctv: Boolean,
  cctvUploadStatus: String?,
  isBusy: Boolean,
  onOpenCctvWall: () -> Unit,
  onDeleteRecording: (CctvRecordingRecord) -> Unit,
) {
  var selectedDate by remember { mutableStateOf("ALL") }
  var selectedCamId by remember { mutableStateOf("ALL") }
  var activePlaybackRecord by remember { mutableStateOf<CctvRecordingRecord?>(null) }
  var pendingDeleteRecord by remember { mutableStateOf<CctvRecordingRecord?>(null) }

  val availableDates = remember(recordings) {
    recordings.mapNotNull { it.dateKey.takeIf(String::isNotBlank) }.distinct().sortedDescending()
  }
  val availableCameras = remember(recordings) {
    recordings
      .filter { it.cameraId.isNotBlank() }
      .distinctBy { it.cameraId }
      .sortedBy { it.cameraLabel.ifBlank { it.cameraId } }
  }
  val filtered = remember(recordings, selectedDate, selectedCamId) {
    recordings.filter { rec ->
      val dateMatch = selectedDate == "ALL" || rec.dateKey == selectedDate
      val camMatch = selectedCamId == "ALL" || rec.cameraId == selectedCamId
      dateMatch && camMatch
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = ArchiveCard,
      border = BorderStroke(1.dp, ArchiveCyan.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "PERSISTENT CLOUD CCTV ARCHIVE",
            color = ArchiveCyan,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            "7 Common Feeds (incl. Dining Area). Bathrooms, Washrooms & Smoking Lounge are permanently excluded.",
            color = ArchiveMuted,
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onOpenCctvWall,
          colors = ButtonDefaults.buttonColors(containerColor = ArchiveCyan, contentColor = Color(0xFF061622)),
          modifier = Modifier.height(48.dp).testTag("archive_launch_live_cctv_button"),
        ) {
          Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Record Live", fontWeight = FontWeight.Bold)
        }
      }
    }

    if (isUploadingCctv || !cctvUploadStatus.isNullOrBlank()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = (if (isUploadingCctv) ArchiveCyan else ArchiveEmerald).copy(alpha = 0.14f),
        border = BorderStroke(1.dp, if (isUploadingCctv) ArchiveCyan else ArchiveEmerald),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (isUploadingCctv) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ArchiveCyan, strokeWidth = 2.dp)
          }
          Text(
            text = cctvUploadStatus ?: "Uploading recording...",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    // Date filter chips
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("FILTER BY DATE", color = ArchiveGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterPillChip(
          label = "All Dates",
          selected = selectedDate == "ALL",
          tag = "cctv_filter_date_all",
          onClick = { selectedDate = "ALL" },
        )
        availableDates.forEach { dKey ->
          FilterPillChip(
            label = dKey,
            selected = selectedDate == dKey,
            tag = "cctv_filter_date_$dKey",
            onClick = { selectedDate = dKey },
          )
        }
      }
    }

    // Camera filter chips
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("FILTER BY CAMERA", color = ArchiveGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterPillChip(
          label = "All Cameras",
          selected = selectedCamId == "ALL",
          tag = "cctv_filter_cam_all",
          onClick = { selectedCamId = "ALL" },
        )
        availableCameras.forEach { camRec ->
          FilterPillChip(
            label = camRec.cameraLabel.ifBlank { camRec.cameraId },
            selected = selectedCamId == camRec.cameraId,
            tag = "cctv_filter_cam_${camRec.cameraId}",
            onClick = { selectedCamId = camRec.cameraId },
          )
        }
      }
    }

    Text(
      "RECORDED CLIPS (${filtered.size})",
      color = ArchiveMuted,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
    )

    if (filtered.isEmpty()) {
      Surface(shape = RoundedCornerShape(14.dp), color = ArchiveCard, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("No recordings match the selected filter.", color = Color.White, fontWeight = FontWeight.SemiBold)
          Text(
            "Open Live 3D CCTV and tap REC on any camera feed to capture and archive real game footage to Firebase Storage.",
            color = ArchiveMuted,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    } else {
      filtered.forEach { rec ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = ArchiveCard,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = rec.cameraLabel.ifBlank { rec.cameraId },
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${CctvPlaybackMath.formatRecordedAt(rec.startedAtMillis)} • ${CctvPlaybackMath.formatDurationMs(rec.durationMillis)} • ${CctvPlaybackMath.formatFileSize(rec.sizeBytes)}",
                color = ArchiveMuted,
                style = MaterialTheme.typography.labelSmall,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Button(
                onClick = { activePlaybackRecord = rec },
                enabled = rec.downloadUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ArchiveGold, contentColor = Color(0xFF10141F)),
                modifier = Modifier.heightIn(min = 48.dp).testTag("play_cctv_${rec.id}"),
              ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Recording", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontWeight = FontWeight.Bold)
              }
              IconButton(
                onClick = { pendingDeleteRecord = rec },
                enabled = !isBusy,
                modifier = Modifier.size(48.dp).testTag("delete_cctv_${rec.id}"),
              ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete Recording", tint = ArchiveDanger)
              }
            }
          }
        }
      }
    }
  }

  activePlaybackRecord?.let { rec ->
    CctvVideoPlaybackDialog(
      record = rec,
      onDismiss = { activePlaybackRecord = null },
    )
  }

  pendingDeleteRecord?.let { rec ->
    AlertDialog(
      onDismissRequest = { pendingDeleteRecord = null },
      containerColor = ArchiveCard,
      title = { Text("Delete CCTV Recording?", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Permanently delete ${rec.cameraLabel} (${CctvPlaybackMath.formatRecordedAt(rec.startedAtMillis)}) from Firebase Storage and Cloud Archive?",
          color = ArchiveMuted,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            pendingDeleteRecord = null
            onDeleteRecording(rec)
          },
        ) {
          Text("Delete", color = ArchiveDanger, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteRecord = null }) {
          Text("Cancel", color = ArchiveGold)
        }
      },
    )
  }
}

@Composable
private fun FilterPillChip(
  label: String,
  selected: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = if (selected) ArchiveCyan else ArchiveElevated,
    border = BorderStroke(1.dp, if (selected) ArchiveCyan else Color.White.copy(alpha = 0.12f)),
    modifier = Modifier
      .heightIn(min = 48.dp)
      .clickable(onClick = onClick)
      .testTag(tag),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
      Text(
        text = label,
        color = if (selected) Color(0xFF061622) else Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun CctvVideoPlaybackDialog(
  record: CctvRecordingRecord,
  onDismiss: () -> Unit,
) {
  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
  var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
  var isBuffering by remember { mutableStateOf(true) }
  var isPlaying by remember { mutableStateOf(false) }
  var isSeeking by remember { mutableStateOf(false) }
  var positionMs by remember { mutableLongStateOf(0L) }
  var durationMs by remember { mutableLongStateOf(record.durationMillis.coerceAtLeast(1000L)) }
  var sliderPosMs by remember { mutableFloatStateOf(0f) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var playbackError by remember { mutableStateOf<String?>(null) }

  fun applySpeed(mp: MediaPlayer?, speed: Float) {
    if (mp == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        mp.playbackParams = mp.playbackParams.setSpeed(speed)
      } catch (_: Exception) {
        // Device codec may reject non-1x speed changes; ignore gracefully
      }
    }
  }

  LaunchedEffect(videoViewRef, isPlaying, isSeeking) {
    while (true) {
      val vv = videoViewRef
      if (vv != null && !isSeeking) {
        val pos = runCatching { vv.currentPosition.toLong() }.getOrDefault(positionMs)
        val dur = runCatching { vv.duration.toLong() }.getOrDefault(0L)
        if (dur > 0L) durationMs = dur
        positionMs = pos
        sliderPosMs = pos.toFloat()
        isPlaying = runCatching { vv.isPlaying }.getOrDefault(false)
      }
      delay(250L)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      runCatching { videoViewRef?.stopPlayback() }
      videoViewRef = null
      mediaPlayerRef = null
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = ArchiveBg,
      border = BorderStroke(1.5.dp, ArchiveCyan.copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 780.dp),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${record.cameraLabel.ifBlank { record.cameraId }} • ARCHIVE PLAYBACK",
              color = ArchiveCyan,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.2.sp,
            )
            Text(
              text = CctvPlaybackMath.formatRecordedAt(record.startedAtMillis),
              color = Color.White,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("close_playback_button")) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Playback", tint = Color.White)
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
          contentAlignment = Alignment.Center,
        ) {
          AndroidView(
            modifier = Modifier.fillMaxSize().testTag("cctv_video_player"),
            factory = { ctx ->
              VideoView(ctx).apply {
                setOnPreparedListener { mp ->
                  mediaPlayerRef = mp
                  isBuffering = false
                  val d = mp.duration.toLong()
                  if (d > 0L) durationMs = d
                  applySpeed(mp, playbackSpeed)
                  start()
                  isPlaying = true
                }
                setOnCompletionListener {
                  isPlaying = false
                  positionMs = durationMs
                  sliderPosMs = durationMs.toFloat()
                }
                setOnErrorListener { _, what, extra ->
                  isBuffering = false
                  isPlaying = false
                  playbackError = "Unable to stream CCTV video (code=$what/$extra)."
                  true
                }
                setVideoURI(Uri.parse(record.downloadUrl))
                videoViewRef = this
              }
            },
          )

          if (isBuffering && playbackError == null) {
            CircularProgressIndicator(color = ArchiveCyan)
          }

          playbackError?.let { err ->
            Surface(
              color = ArchiveDanger.copy(alpha = 0.2f),
              border = BorderStroke(1.dp, ArchiveDanger),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.padding(16.dp),
            ) {
              Text(err, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
          }
        }

        // Timeline Scrubber
        Column {
          Slider(
            value = sliderPosMs.coerceIn(0f, durationMs.coerceAtLeast(1000L).toFloat()),
            onValueChange = { next ->
              isSeeking = true
              sliderPosMs = next
            },
            onValueChangeFinished = {
              val target = CctvPlaybackMath.clampSeekMs(sliderPosMs.toLong(), durationMs)
              videoViewRef?.seekTo(target.toInt())
              positionMs = target
              isSeeking = false
            },
            valueRange = 0f..durationMs.coerceAtLeast(1000L).toFloat(),
            colors = SliderDefaults.colors(
              thumbColor = ArchiveCyan,
              activeTrackColor = ArchiveCyan,
              inactiveTrackColor = ArchiveElevated,
            ),
            modifier = Modifier.fillMaxWidth().testTag("cctv_timeline_slider"),
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              CctvPlaybackMath.formatDurationMs(sliderPosMs.toLong()),
              color = ArchiveMuted,
              style = MaterialTheme.typography.labelSmall,
            )
            Text(
              CctvPlaybackMath.formatDurationMs(durationMs),
              color = ArchiveMuted,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }

        // Transport & Speed Controls
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = {
              val target = CctvPlaybackMath.rewind5s(positionMs, durationMs)
              videoViewRef?.seekTo(target.toInt())
              positionMs = target
              sliderPosMs = target.toFloat()
            },
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.height(48.dp).testTag("cctv_rewind_button"),
          ) {
            Icon(Icons.Rounded.FastRewind, contentDescription = "Rewind 5 seconds", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("-5s", color = Color.White)
          }

          OutlinedButton(
            onClick = {
              videoViewRef?.seekTo(0)
              videoViewRef?.start()
              applySpeed(mediaPlayerRef, playbackSpeed)
              positionMs = 0L
              sliderPosMs = 0f
              isPlaying = true
            },
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.height(48.dp).testTag("cctv_restart_button"),
          ) {
            Icon(Icons.Rounded.Replay, contentDescription = "Restart", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Restart", color = Color.White)
          }

          Button(
            onClick = {
              val vv = videoViewRef ?: return@Button
              if (vv.isPlaying) {
                vv.pause()
                isPlaying = false
              } else {
                vv.start()
                applySpeed(mediaPlayerRef, playbackSpeed)
                isPlaying = true
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ArchiveCyan, contentColor = Color(0xFF061622)),
            modifier = Modifier.height(48.dp).testTag("cctv_play_pause_button"),
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.width(4.dp))

          CctvPlaybackMath.SUPPORTED_SPEEDS.forEach { spd ->
            val selected = playbackSpeed == spd
            val label = if (spd == 1.0f) "1x" else if (spd == 2.0f) "2x" else "${spd}x"
            FilterPillChip(
              label = label,
              selected = selected,
              tag = "cctv_speed_${label}",
              onClick = {
                playbackSpeed = spd
                applySpeed(mediaPlayerRef, spd)
              },
            )
          }
        }
      }
    }
  }
}
