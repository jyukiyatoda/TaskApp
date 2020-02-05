package com.example.taskapp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.fab
import kotlinx.android.synthetic.main.content_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import java.util.*
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context

const val EXTRA_TASK="com.example.taskapp.TASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener=object:RealmChangeListener<Realm>{
        override  fun onChange(element:Realm){
            reloadListView()
        }
    }
    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        //Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)
        //ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //ListViewをタップした時の処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            //入力・編集する画面に推移
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        //ListViewを長押しした時の処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            //タスクを削除する
            val task = parent.adapter.getItem(position) as Task
           //ダイアログを表示
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title+"を削除しますか")

            builder.setPositiveButton("OK") { _, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent=Intent(applicationContext,TaskAlarmReceiver::class.java)
                val resultPendingIntent=PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                val alarmManager=getSystemService(Context.ALARM_SERVICE)as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()

            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }
    private fun reloadListView(){
        //Realmデータベースから「全てのデータを取得して新しい日時順を並べた結果」を取得
        val taskRealmResults=mRealm.where(Task::class.java).findAll().sort("date",Sort.DESCENDING)

        //上記の結果をTaskListとしてセットする
        mTaskAdapter.taskList=mRealm.copyFromRealm(taskRealmResults)

        //TaskのListView用のアダプタに渡す
        listView1.adapter=mTaskAdapter

        //ひょうじを更新するために、アダプタにーデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()

    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }
    private fun addTaskForTest(){
        val task=Task()
        task.title="作業"
        task.contents="プログラムを書いてPUSHする"
        task.date= Date()
        task.id=0
        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(task)
        mRealm.commitTransaction()
    }
}

