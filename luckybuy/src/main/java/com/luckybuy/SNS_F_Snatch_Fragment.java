package com.luckybuy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.luckybuy.adapter.AwardUnveilAdapter;
import com.luckybuy.adapter.F_SNS_SnatchAdapter;
import com.luckybuy.adapter.ListBaseAdapter;
import com.luckybuy.login.LoginUserUtils;
import com.luckybuy.model.AwardModel;
import com.luckybuy.model.SnatchAwardModel;
import com.luckybuy.network.ParseData;
import com.luckybuy.util.Constant;
import com.luckybuy.util.RefreshTask;
import com.luckybuy.util.RefreshUtil;
import com.luckybuy.util.Utility;

import org.xutils.common.Callback;
import org.xutils.ex.HttpException;
import org.xutils.http.RequestParams;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhiPeng.S on 2016/5/16.
 */
@ContentView(R.layout.fragment_snatch)
public class SNS_F_Snatch_Fragment extends BaseFragment implements AdapterView.OnItemClickListener,RefreshUtil,PullToRefreshBase.OnRefreshListener2<ListView>{

    public static SNS_F_Snatch_Fragment newInstance(long user_id_friends) {
        SNS_F_Snatch_Fragment fragment = new SNS_F_Snatch_Fragment();
        Bundle args = new Bundle();
        args.putLong("user_id_friends", user_id_friends);
        fragment.setArguments(args);
        return fragment;
    }

    private long user_id;
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setShowList();
        user_id = getArguments().getLong("user_id_friends",0);
        if(user_id == 0) return;
        getShowListInfo("", "10",user_id+"",false);
    }

    @ViewInject(R.id.blank_buy_record_rl)
    private RelativeLayout blank_buy_record_rl;

    private void updateView(boolean hasData){
        if(!hasData){
            blank_buy_record_rl.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            return;
        }

        blank_buy_record_rl.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) msg.obj;
            switch (msg.what) {
                case R.id.AWARD_SUCCESS:
                    if (!result.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        List<SnatchAwardModel> showlist =
                                (List<SnatchAwardModel>) result.get(Constant.AWARD_LIST);
                        boolean orientation = (boolean) result.get("pull_up");
                        if(!orientation) datas.clear();

                        int size = showlist == null ? 0 : showlist.size();
                        for (int i = 0; i < size; i++) datas.add(showlist.get(i));

                        adapter.setData(datas);
                        adapter.notifyDataSetChanged();
                        updateView(!datas.isEmpty());
                    } else {
                        String returnContent = (String) result.get(Constant.RETURN_CONTENT);
                        Utility.toastShow(x.app(), returnContent);
                    }
                    break;
            }
        }
    };

    @ViewInject(R.id.snatch_lv)
    private PullToRefreshListView listView;
    private ListBaseAdapter<SnatchAwardModel> adapter;
    private List<SnatchAwardModel> datas;
    private void setShowList(){
        listView.setAdapter(getShowListAdapter());
        listView.setOnItemClickListener(this);
        listView.setMode(PullToRefreshBase.Mode.BOTH);
        listView.setOnRefreshListener(this);
    }

    private ListBaseAdapter<SnatchAwardModel> getShowListAdapter(){
        datas = new ArrayList<>();
        adapter = new F_SNS_SnatchAdapter(getActivity(), datas);
        return adapter;
    }

    @Override
    public void getShowListInfo(String lastid, String pagesize, String uidx, final boolean orientation) {
        RequestParams params = new RequestParams(Constant.getBaseUrl() + "page/ucenter/playlist.ashx?listtype=all");

        params.addQueryStringParameter("uidx", uidx);
        params.addQueryStringParameter("lasttimeid",lastid);
        params.addQueryStringParameter("pagesize",pagesize);
        x.http().get(params, new Callback.CommonCallback<String>() {

            @Override
            public void onSuccess(String result) {
                Map<String, Object> resultMap = ParseData.parseSnatchAwardInfo(result);
                resultMap.put("pull_up", orientation);
                mHandler.obtainMessage(R.id.AWARD_SUCCESS, resultMap).sendToTarget();
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("error", ex.getMessage());
                if (ex instanceof HttpException) { // 网络错误
                    HttpException httpEx = (HttpException) ex;
                    int responseCode = httpEx.getCode();
                    String responseMsg = httpEx.getMessage();
                    String errorResult = httpEx.getResult();
                    // ...
                } else { // 其他错误
                    // ...
                }
            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundle bundle = new Bundle();
        AwardModel model = new AwardModel();
        model.setIdx(datas.get(position-1).getGoodid());
        model.setTimeid(datas.get(position-1).getTimesid());
        bundle.putSerializable("bundle", model);
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private RefreshTask refreshTask;
    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        String lastid = "";
        refreshTask = new RefreshTask(getActivity(),listView,this);
        refreshTask.setLoadOrientation(lastid,false);
        refreshTask.setUserId(String.valueOf(user_id));
        refreshTask.execute();
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
        String lastid = String.valueOf(adapter.getLastid());
        refreshTask = new RefreshTask(getActivity(),listView,this);
        refreshTask.setLoadOrientation(lastid,true);
        refreshTask.setUserId(String.valueOf(user_id));
        refreshTask.execute();
    }


}
