package com.warehouse.monitor.db;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.warehouse.monitor.model.Scene;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SceneDao_Impl implements SceneDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Scene> __insertionAdapterOfScene;

  private final EntityDeletionOrUpdateAdapter<Scene> __deletionAdapterOfScene;

  private final EntityDeletionOrUpdateAdapter<Scene> __updateAdapterOfScene;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastTriggerTime;

  public SceneDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScene = new EntityInsertionAdapter<Scene>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `scenes` (`id`,`name`,`icon`,`color`,`deviceIds`,`deviceStates`,`isEnabled`,`createTime`,`lastTriggerTime`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Scene value) {
        stmt.bindLong(1, value.getId());
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        if (value.getIcon() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getIcon());
        }
        stmt.bindLong(4, value.getColor());
        if (value.getDeviceIds() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getDeviceIds());
        }
        if (value.getDeviceStates() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getDeviceStates());
        }
        final int _tmp = value.isEnabled() ? 1 : 0;
        stmt.bindLong(7, _tmp);
        stmt.bindLong(8, value.getCreateTime());
        stmt.bindLong(9, value.getLastTriggerTime());
      }
    };
    this.__deletionAdapterOfScene = new EntityDeletionOrUpdateAdapter<Scene>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `scenes` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Scene value) {
        stmt.bindLong(1, value.getId());
      }
    };
    this.__updateAdapterOfScene = new EntityDeletionOrUpdateAdapter<Scene>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `scenes` SET `id` = ?,`name` = ?,`icon` = ?,`color` = ?,`deviceIds` = ?,`deviceStates` = ?,`isEnabled` = ?,`createTime` = ?,`lastTriggerTime` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Scene value) {
        stmt.bindLong(1, value.getId());
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        if (value.getIcon() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getIcon());
        }
        stmt.bindLong(4, value.getColor());
        if (value.getDeviceIds() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getDeviceIds());
        }
        if (value.getDeviceStates() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getDeviceStates());
        }
        final int _tmp = value.isEnabled() ? 1 : 0;
        stmt.bindLong(7, _tmp);
        stmt.bindLong(8, value.getCreateTime());
        stmt.bindLong(9, value.getLastTriggerTime());
        stmt.bindLong(10, value.getId());
      }
    };
    this.__preparedStmtOfUpdateLastTriggerTime = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE scenes SET lastTriggerTime = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(final Scene scene) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfScene.insert(scene);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final Scene scene) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfScene.handle(scene);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Scene scene) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfScene.handle(scene);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateLastTriggerTime(final int id, final long time) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastTriggerTime.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, time);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, id);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateLastTriggerTime.release(_stmt);
    }
  }

  @Override
  public List<Scene> getAllScenes() {
    final String _sql = "SELECT * FROM scenes ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
      final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
      final int _cursorIndexOfDeviceIds = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceIds");
      final int _cursorIndexOfDeviceStates = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceStates");
      final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
      final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
      final int _cursorIndexOfLastTriggerTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastTriggerTime");
      final List<Scene> _result = new ArrayList<Scene>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Scene _item;
        _item = new Scene();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _item.setName(_tmpName);
        final String _tmpIcon;
        if (_cursor.isNull(_cursorIndexOfIcon)) {
          _tmpIcon = null;
        } else {
          _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
        }
        _item.setIcon(_tmpIcon);
        final int _tmpColor;
        _tmpColor = _cursor.getInt(_cursorIndexOfColor);
        _item.setColor(_tmpColor);
        final String _tmpDeviceIds;
        if (_cursor.isNull(_cursorIndexOfDeviceIds)) {
          _tmpDeviceIds = null;
        } else {
          _tmpDeviceIds = _cursor.getString(_cursorIndexOfDeviceIds);
        }
        _item.setDeviceIds(_tmpDeviceIds);
        final String _tmpDeviceStates;
        if (_cursor.isNull(_cursorIndexOfDeviceStates)) {
          _tmpDeviceStates = null;
        } else {
          _tmpDeviceStates = _cursor.getString(_cursorIndexOfDeviceStates);
        }
        _item.setDeviceStates(_tmpDeviceStates);
        final boolean _tmpIsEnabled;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
        _tmpIsEnabled = _tmp != 0;
        _item.setEnabled(_tmpIsEnabled);
        final long _tmpCreateTime;
        _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
        _item.setCreateTime(_tmpCreateTime);
        final long _tmpLastTriggerTime;
        _tmpLastTriggerTime = _cursor.getLong(_cursorIndexOfLastTriggerTime);
        _item.setLastTriggerTime(_tmpLastTriggerTime);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Scene> getEnabledScenes() {
    final String _sql = "SELECT * FROM scenes WHERE isEnabled = 1 ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
      final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
      final int _cursorIndexOfDeviceIds = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceIds");
      final int _cursorIndexOfDeviceStates = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceStates");
      final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
      final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
      final int _cursorIndexOfLastTriggerTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastTriggerTime");
      final List<Scene> _result = new ArrayList<Scene>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Scene _item;
        _item = new Scene();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _item.setName(_tmpName);
        final String _tmpIcon;
        if (_cursor.isNull(_cursorIndexOfIcon)) {
          _tmpIcon = null;
        } else {
          _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
        }
        _item.setIcon(_tmpIcon);
        final int _tmpColor;
        _tmpColor = _cursor.getInt(_cursorIndexOfColor);
        _item.setColor(_tmpColor);
        final String _tmpDeviceIds;
        if (_cursor.isNull(_cursorIndexOfDeviceIds)) {
          _tmpDeviceIds = null;
        } else {
          _tmpDeviceIds = _cursor.getString(_cursorIndexOfDeviceIds);
        }
        _item.setDeviceIds(_tmpDeviceIds);
        final String _tmpDeviceStates;
        if (_cursor.isNull(_cursorIndexOfDeviceStates)) {
          _tmpDeviceStates = null;
        } else {
          _tmpDeviceStates = _cursor.getString(_cursorIndexOfDeviceStates);
        }
        _item.setDeviceStates(_tmpDeviceStates);
        final boolean _tmpIsEnabled;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
        _tmpIsEnabled = _tmp != 0;
        _item.setEnabled(_tmpIsEnabled);
        final long _tmpCreateTime;
        _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
        _item.setCreateTime(_tmpCreateTime);
        final long _tmpLastTriggerTime;
        _tmpLastTriggerTime = _cursor.getLong(_cursorIndexOfLastTriggerTime);
        _item.setLastTriggerTime(_tmpLastTriggerTime);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Scene getSceneById(final int id) {
    final String _sql = "SELECT * FROM scenes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
      final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
      final int _cursorIndexOfDeviceIds = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceIds");
      final int _cursorIndexOfDeviceStates = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceStates");
      final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
      final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
      final int _cursorIndexOfLastTriggerTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastTriggerTime");
      final Scene _result;
      if(_cursor.moveToFirst()) {
        _result = new Scene();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _result.setName(_tmpName);
        final String _tmpIcon;
        if (_cursor.isNull(_cursorIndexOfIcon)) {
          _tmpIcon = null;
        } else {
          _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
        }
        _result.setIcon(_tmpIcon);
        final int _tmpColor;
        _tmpColor = _cursor.getInt(_cursorIndexOfColor);
        _result.setColor(_tmpColor);
        final String _tmpDeviceIds;
        if (_cursor.isNull(_cursorIndexOfDeviceIds)) {
          _tmpDeviceIds = null;
        } else {
          _tmpDeviceIds = _cursor.getString(_cursorIndexOfDeviceIds);
        }
        _result.setDeviceIds(_tmpDeviceIds);
        final String _tmpDeviceStates;
        if (_cursor.isNull(_cursorIndexOfDeviceStates)) {
          _tmpDeviceStates = null;
        } else {
          _tmpDeviceStates = _cursor.getString(_cursorIndexOfDeviceStates);
        }
        _result.setDeviceStates(_tmpDeviceStates);
        final boolean _tmpIsEnabled;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
        _tmpIsEnabled = _tmp != 0;
        _result.setEnabled(_tmpIsEnabled);
        final long _tmpCreateTime;
        _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
        _result.setCreateTime(_tmpCreateTime);
        final long _tmpLastTriggerTime;
        _tmpLastTriggerTime = _cursor.getLong(_cursorIndexOfLastTriggerTime);
        _result.setLastTriggerTime(_tmpLastTriggerTime);
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
}
