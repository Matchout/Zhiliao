package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Helper;
import com.shatyuka.zhiliao.xposed.XC_MethodHook;
import com.shatyuka.zhiliao.xposed.XposedBridge;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 隐藏推荐页置顶热门（FeedTopHot = feed 列表第一项）。
 * 数据层过滤：在 feed 刷新数据装配时移除第一项 FeedTopHot，
 * 不触碰 autojackson 反序列化基类（BaseObjectStdDeserializer.deserialize），
 * 从机制上消除"开启后导致知乎全局登录/账号链路数据异常"的隐患。
 */
public class FeedTopHotBanner implements IHook {

    static Class feedTopHot;
    static Class basePagingFragment;
    static Class feedList;
    static Field feedListData;

    @Override
    public String getName() {
        return "隐藏推荐页置顶热门";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        try {
            feedTopHot = classLoader.loadClass("com.zhihu.android.api.model.FeedTopHot");
        } catch (ClassNotFoundException ignore) {
        }
        try {
            basePagingFragment = classLoader.loadClass("com.zhihu.android.app.ui.fragment.paging.BasePagingFragment");
            feedList = classLoader.loadClass("com.zhihu.android.api.model.FeedList");
            feedListData = feedList.getField("data");
        } catch (ClassNotFoundException | NoSuchFieldException ignore) {
        }
    }

    @Override
    public void hook() throws Throwable {
        if (feedTopHot == null || basePagingFragment == null || feedListData == null) return;

        XposedBridge.hookAllMethods(basePagingFragment, "postRefreshSucceed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!Helper.prefs.getBoolean("switch_mainswitch", false)
                        || !Helper.prefs.getBoolean("switch_feedtophot", false)) return;
                if (param.args.length == 0 || param.args[0] == null
                        || !feedList.isInstance(param.args[0])) return;
                try {
                    @SuppressWarnings("unchecked")
                    List<Object> data = (List<Object>) feedListData.get(param.args[0]);
                    if (data != null && !data.isEmpty()
                            && feedTopHot.isInstance(data.get(0))) {
                        data.remove(0);          // 移除置顶热门（feed 首项）
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        });
    }
}
