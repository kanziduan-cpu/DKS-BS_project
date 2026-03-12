package com.warehouse.monitor.db;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.warehouse.monitor.model.Device;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DeviceDao_Impl implements DeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Device> __insertionAdapterOfDevice;

  private final EntityDeletionOrUpdateAdapter<Device> __deletionAdapterOfDevice;

  private final EntityDeletionOrUpdateAdapter<Device> __updateAdapterOfDevice;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDeviceStatus;

  public DeviceDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDevice = new EntityInsertionAdapter<Device>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `devices` (`deviceId`,`name`,`type`,`status`,`isRunning`,`warehouseId`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Device value) {
        if (value.getDeviceId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getDeviceId());
        }
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        if (value.getType() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, __DeviceType_enumToString(value.getType()));
        }
        if (value.getStatus() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, __DeviceStatus_enumToString(value.getStatus()));
        }
        final int _tmp = value.isRunning() ? 1 : 0;
        stmt.bindLong(5, _tmp);
        if (value.getWarehouseId() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getWarehouseId());
        }
      }
    };
    this.__deletionAdapterOfDevice = new EntityDeletionOrUpdateAdapter<Device>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `devices` WHERE `deviceId` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Device value) {
        if (value.getDeviceId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getDeviceId());
        }
      }
    };
    this.__updateAdapterOfDevice = new EntityDeletionOrUpdateAdapter<Device>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `devices` SET `deviceId` = ?,`name` = ?,`type` = ?,`status` = ?,`isRunning` = ?,`warehouseId` = ? WHERE `deviceId` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Device value) {
        if (value.getDeviceId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getDeviceId());
        }
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        if (value.getType() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, __DeviceType_enumToString(value.getType()));
        }
        if (value.getStatus() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, __DeviceStatus_enumToString(value.getStatus()));
        }
        final int _tmp = value.isRunning() ? 1 : 0;
        stmt.bindLong(5, _tmp);
        if (value.getWarehouseId() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getWarehouseId());
        }
        if (value.getDeviceId() == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.getDeviceId());
        }
      }
    };
    this.__preparedStmtOfUpdateDeviceStatus = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE devices SET isRunning = ? WHERE deviceId = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertDevice(final Device device) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfDevice.insert(device);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteDevice(final Device device) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfDevice.handle(device);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateDevice(final Device device) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfDevice.handle(device);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateDeviceStatus(final String deviceId, final boolean isRunning) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDeviceStatus.acquire();
    int _argIndex = 1;
    final int _tmp = isRunning ? 1 : 0;
    _stmt.bindLong(_argIndex, _tmp);
    _argIndex = 2;
    if (deviceId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, deviceId);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateDeviceStatus.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Device>> getAllDevicesLive() {
    final String _sql = "SELECT * FROM devices";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"devices"}, false, new Callable<List<Device>>() {
      @Override
      public List<Device> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouseId");
          final List<Device> _result = new ArrayList<Device>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Device _item;
            _item = new Device();
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            _item.setDeviceId(_tmpDeviceId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            _item.setName(_tmpName);
            final Device.DeviceType _tmpType;
            _tmpType = __DeviceType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            _item.setType(_tmpType);
            final Device.DeviceStatus _tmpStatus;
            _tmpStatus = __DeviceStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            _item.setStatus(_tmpStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            _item.setRunning(_tmpIsRunning);
            final String _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getString(_cursorIndexOfWarehouseId);
            }
            _item.setWarehouseId(_tmpWarehouseId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<Device> getAllDevices() {
    final String _sql = "SELECT * FROM devices";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
      final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouseId");
      final List<Device> _result = new ArrayList<Device>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Device _item;
        _item = new Device();
        final String _tmpDeviceId;
        if (_cursor.isNull(_cursorIndexOfDeviceId)) {
          _tmpDeviceId = null;
        } else {
          _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
        }
        _item.setDeviceId(_tmpDeviceId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _item.setName(_tmpName);
        final Device.DeviceType _tmpType;
        _tmpType = __DeviceType_stringToEnum(_cursor.getString(_cursorIndexOfType));
        _item.setType(_tmpType);
        final Device.DeviceStatus _tmpStatus;
        _tmpStatus = __DeviceStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
        _item.setStatus(_tmpStatus);
        final boolean _tmpIsRunning;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
        _tmpIsRunning = _tmp != 0;
        _item.setRunning(_tmpIsRunning);
        final String _tmpWarehouseId;
        if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
          _tmpWarehouseId = null;
        } else {
          _tmpWarehouseId = _cursor.getString(_cursorIndexOfWarehouseId);
        }
        _item.setWarehouseId(_tmpWarehouseId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Device getDeviceById(final String deviceId) {
    final String _sql = "SELECT * FROM devices WHERE deviceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (deviceId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, deviceId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
      final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouseId");
      final Device _result;
      if(_cursor.moveToFirst()) {
        _result = new Device();
        final String _tmpDeviceId;
        if (_cursor.isNull(_cursorIndexOfDeviceId)) {
          _tmpDeviceId = null;
        } else {
          _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
        }
        _result.setDeviceId(_tmpDeviceId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _result.setName(_tmpName);
        final Device.DeviceType _tmpType;
        _tmpType = __DeviceType_stringToEnum(_cursor.getString(_cursorIndexOfType));
        _result.setType(_tmpType);
        final Device.DeviceStatus _tmpStatus;
        _tmpStatus = __DeviceStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
        _result.setStatus(_tmpStatus);
        final boolean _tmpIsRunning;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
        _tmpIsRunning = _tmp != 0;
        _result.setRunning(_tmpIsRunning);
        final String _tmpWarehouseId;
        if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
          _tmpWarehouseId = null;
        } else {
          _tmpWarehouseId = _cursor.getString(_cursorIndexOfWarehouseId);
        }
        _result.setWarehouseId(_tmpWarehouseId);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __DeviceType_enumToString(final Device.DeviceType _value) {
    if (_value == null) {
      return null;
    } switch (_value) {
      case VENTILATION_FAN: return "VENTILATION_FAN";
      case WATER_PUMP: return "WATER_PUMP";
      case DEHUMIDIFIER: return "DEHUMIDIFIER";
      case EXHAUST_DEVICE: return "EXHAUST_DEVICE";
      case LIGHTING: return "LIGHTING";
      case STM32_EDGE: return "STM32_EDGE";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __DeviceStatus_enumToString(final Device.DeviceStatus _value) {
    if (_value == null) {
      return null;
    } switch (_value) {
      case ONLINE: return "ONLINE";
      case OFFLINE: return "OFFLINE";
      case ERROR: return "ERROR";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private Device.DeviceType __DeviceType_stringToEnum(final String _value) {
    if (_value == null) {
      return null;
    } switch (_value) {
      case "VENTILATION_FAN": return Device.DeviceType.VENTILATION_FAN;
      case "WATER_PUMP": return Device.DeviceType.WATER_PUMP;
      case "DEHUMIDIFIER": return Device.DeviceType.DEHUMIDIFIER;
      case "EXHAUST_DEVICE": return Device.DeviceType.EXHAUST_DEVICE;
      case "LIGHTING": return Device.DeviceType.LIGHTING;
      case "STM32_EDGE": return Device.DeviceType.STM32_EDGE;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private Device.DeviceStatus __DeviceStatus_stringToEnum(final String _value) {
    if (_value == null) {
      return null;
    } switch (_value) {
      case "ONLINE": return Device.DeviceStatus.ONLINE;
      case "OFFLINE": return Device.DeviceStatus.OFFLINE;
      case "ERROR": return Device.DeviceStatus.ERROR;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
