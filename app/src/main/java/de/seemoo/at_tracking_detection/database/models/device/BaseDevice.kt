package de.seemoo.at_tracking_detection.database.models.device

import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.room.*
import de.seemoo.at_tracking_detection.database.models.device.types.AirTag
import de.seemoo.at_tracking_detection.database.models.device.types.Unknown
import de.seemoo.at_tracking_detection.util.converter.DateTimeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.experimental.and

@Entity(tableName = "device", indices = [Index(value = ["address"], unique = true)])
@TypeConverters(DateTimeConverter::class)
data class BaseDevice(
    @PrimaryKey(autoGenerate = true) var deviceId: Int,
    @ColumnInfo(name = "address") var address: String,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "ignore") val ignore: Boolean,
    @ColumnInfo(name = "connectable") val connectable: Boolean,
    @ColumnInfo(name = "payloadData") val payloadData: Byte?,
    @ColumnInfo(name = "firstDiscovery") val firstDiscovery: LocalDateTime,
    @ColumnInfo(name = "lastSeen") var lastSeen: LocalDateTime,
    @ColumnInfo(name = "notificationSent") var notificationSent: Boolean,
    @ColumnInfo(name = "lastNotificationSent") var lastNotificationSent: LocalDateTime?,
    @ColumnInfo(name = "deviceType") val deviceType: DeviceType?
) {

    constructor(
        address: String,
        ignore: Boolean,
        connectable: Boolean,
        payloadData: Byte?,
        firstDiscovery: LocalDateTime,
        lastSeen: LocalDateTime,
        deviceType: DeviceType
    ) : this(
        0,
        address,
        null,
        ignore,
        connectable,
        payloadData,
        firstDiscovery,
        lastSeen,
        false,
        null,
        deviceType
    )

    constructor(scanResult: ScanResult) : this(
        0,
        scanResult.device.address,
        scanResult.scanRecord?.deviceName,
        false,
        scanResult.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scanResult.isConnectable
            } else {
                false
            }
        },
        scanResult.scanRecord?.getManufacturerSpecificData(76)?.get(2),
        LocalDateTime.now(), LocalDateTime.now(), false, null,
        DeviceManager.getDeviceType(scanResult)
    )

    fun getDeviceNameWithID(): String = name ?: device.defaultDeviceNameWithId

    @Ignore
    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

    @Ignore
    val device: Device = when (deviceType) {
        DeviceType.AIRTAG -> AirTag(deviceId)
        DeviceType.UNKNOWN -> Unknown(deviceId)
        else -> {
            // For backwards compatibility
            if (payloadData?.and(0x10)?.toInt() != 0 && connectable) {
                AirTag(deviceId)
            } else {
                Unknown(deviceId)
            }
        }
    }

    fun getFormattedDiscoveryDate(): String = firstDiscovery.format(dateTimeFormatter)

    fun getFormattedLastSeenDate(): String = lastSeen.format(dateTimeFormatter)
}