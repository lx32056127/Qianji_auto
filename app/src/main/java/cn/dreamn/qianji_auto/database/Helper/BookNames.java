/*
 * Copyright (C) 2021 dreamn(dream@dreamn.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.dreamn.qianji_auto.database.Helper;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.customview.DialogCustomViewExtKt;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.database.DbManger;
import cn.dreamn.qianji_auto.database.Table.BookName;
import cn.dreamn.qianji_auto.ui.adapter.DataSelectListAdapter;
import cn.dreamn.qianji_auto.utils.runUtils.Task;


public class BookNames {


    public static String getDefault() {
        MMKV mmkv = MMKV.defaultMMKV();
        return mmkv.getString("defaultBookName", "默认账本");
    }

    public static void change(String bookName) {
        MMKV mmkv = MMKV.defaultMMKV();
        mmkv.encode("defaultBookName", bookName);
    }

    public interface getBookBundle{
        void onGet(Bundle bundle);
    }
    public interface getBookBundles{
        void onGet(Bundle[] bundle);
    }
    public interface getBookStrings{
        void onGet(String[] bundle);
    }
    public interface getBookInt{
        void onGet(int length);
    }
    public interface whenFinish{
        void onFinish();
    }
    public static void getOne(String name,getBookBundle getBook) {
        Task.onThread(()->{
            BookName[] bookNames = DbManger.db.BookNameDao().get(name);
            Bundle bundle = new Bundle();
            if (bookNames != null && bookNames.length != 0) {
                bundle.putString("icon", bookNames[0].icon);
                bundle.putString("name", bookNames[0].name);
                bundle.putString("book_id", bookNames[0].book_id);
            }
           getBook.onGet(bundle);
        });

    }

    public static void getAll(getBookStrings getBook) {

        Task.onThread(()->{
            BookName[] bookNames = DbManger.db.BookNameDao().getAll();
        String[] result = new String[bookNames.length + 1];
        for (int i = 0; i < bookNames.length; i++) {
            result[i] = bookNames[i].name;
        }
        result[bookNames.length] = "默认账本";
            getBook.onGet(result);
        });
    }



    public static void getAllIcon(Boolean add,getBookBundles getBook) {
        Task.onThread(()->{
            BookName[] bookNames = DbManger.db.BookNameDao().getAll();
        ArrayList<Bundle> bundleArrayList = new ArrayList<>();

        for (BookName bookName : bookNames) {
            Bundle bundle = new Bundle();
            bundle.putString("name", bookName.name);
            bundle.putInt("id", bookName.id);
            String bid = bookName.book_id;
            if (bid == null || bid.equals("")) bid = "-1";
            bundle.putString("book_id", bid);
            //http://res.qianjiapp.com/headerimages2/maarten-van-den-heuvel-7RyfX2BHoXU-unsplash.jpg!headerimages2

            if(bookName.icon==null|| bookName.icon.equals("")){
                bundle.putString("cover", "http://res.qianjiapp.com/headerimages2/maarten-van-den-heuvel-7RyfX2BHoXU-unsplash.jpg!headerimages2");
            }else{
                bundle.putString("cover", bookName.icon);
            }


            bundleArrayList.add(bundle);
        }


        if (bundleArrayList.size() == 0 && add) {
            Bundle bundle = new Bundle();
            bundle.putString("name", "默认账本");
            bundle.putInt("id", -1);
            bundle.putString("book_id", "-1");
            bundle.putString("cover", "http://res.qianjiapp.com/headerimages2/maarten-van-den-heuvel-7RyfX2BHoXU-unsplash.jpg!headerimages2");
            bundleArrayList.add(bundle);
        }


            getBook.onGet(bundleArrayList.toArray(new Bundle[0]));
        });

    }

    public static void del(int id,whenFinish when) {
        Task.onThread(()->{
            DbManger.db.BookNameDao().del(id);
            when.onFinish();
        });
    }

    public static void upd(int id, String bookName,whenFinish when) {
        Task.onThread(()->{
            DbManger.db.BookNameDao().update(id, bookName);
            when.onFinish();
        });
    }

    public static void add(String bookName,whenFinish when) {

        Task.onThread(()->{
            DbManger.db.BookNameDao().add(bookName, "http://res.qianjiapp.com/headerimages2/maarten-van-den-heuvel-7RyfX2BHoXU-unsplash.jpg!headerimages2", String.valueOf(System.currentTimeMillis()));
            when.onFinish();
        });
    }

    public static void add(String bookName, String icon,final String book_id,whenFinish when) {
        Task.onThread(()->{

        String bid=book_id;
        if (bid == null || bid.equals("")) {
            bid = String.valueOf(System.currentTimeMillis());
        }
        DbManger.db.BookNameDao().add(bookName, icon, bid);
        when.onFinish();
        });
    }

    public static void clean(whenFinish when) {
        Task.onThread(()->{
            DbManger.db.BookNameDao().clean();
            when.onFinish();
        });
    }

    public static void getAllLen(getBookInt getBook) {
        Task.onThread(()->{
        BookName[] bookNames = DbManger.db.BookNameDao().getAll();
        getBook.onGet(bookNames.length);

        });

    }


    public interface BookSelect {
        void onSelect(Bundle bundle);
    }

    public static void showBookSelect(Context context, String title, BookSelect getOne ) {

        LayoutInflater factory = LayoutInflater.from(context);
        final View textEntryView = factory.inflate(R.layout.list_data, null);

        //final TextView list_title = textEntryView.findViewById(R.id.list_title);

        final ListView list_view = textEntryView.findViewById(R.id.list_view);

        final Handler mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                Bundle[] books=(Bundle[])msg.obj;
                DataSelectListAdapter adapter = new DataSelectListAdapter(context,books);//listdata和str均可
                list_view.setAdapter(adapter);

                MaterialDialog dialog = new MaterialDialog(context, MaterialDialog.getDEFAULT_BEHAVIOR());
                dialog.title(null,title);

                DialogCustomViewExtKt.customView(dialog, null, textEntryView,
                        false, true, false, false);
                dialog.show();


                list_view.setOnItemClickListener((parent, view, position, id) -> {
                    getOne.onSelect(books[position]);
                    dialog.dismiss();
                });
            }
        };

        getAllIcon(false,books -> {
            Message message=new Message();
            message.obj=books;
            mHandler.sendMessage(message);
        });




    }
}