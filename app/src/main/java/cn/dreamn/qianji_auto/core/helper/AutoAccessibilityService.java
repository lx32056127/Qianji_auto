package cn.dreamn.qianji_auto.core.helper;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

import cn.dreamn.qianji_auto.core.db.Cache;
import cn.dreamn.qianji_auto.core.utils.Caches;
import cn.dreamn.qianji_auto.core.utils.Regex;
import cn.dreamn.qianji_auto.core.utils.ServerManger;
import cn.dreamn.qianji_auto.utils.tools.Logs;

public class AutoAccessibilityService extends AccessibilityService {

    public static boolean  isDebug = true;

    private static boolean isStart=false;

    private  List<String> nodeList;
    private  List<String> nodeListId;

    private String packageName="";
    private String className="";
    private  int eventType=0;

    public AutoAccessibilityService() {
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStart=false;
        Logs.d("辅助功能已停用");
    }



    public static boolean isStart(){return isStart;}

    public void onInterrupt() {
        //isStart=false;
        Logs.d("辅助功能被中断...");
    }

    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if(!isStart)return;//服务未启动

        this.packageName = accessibilityEvent.getPackageName().toString();
        this.className = accessibilityEvent.getClassName().toString();
        this.eventType = accessibilityEvent.getEventType();

        Logs.d("Qianji_Screen","------------Start-----------" );
        Logs.d("Qianji_Screen","packageName:" + this.packageName);
        Logs.d("Qianji_Screen","className:" + this.packageName);
        Logs.d("Qianji_Screen","eventType:" + this.eventType);



        if(eventType!=AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED&&eventType!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return;

        if(!packageName.equals("com.tencent.mm")&&!packageName.equals("com.eg.android.AlipayGphone"))return;

        AccessibilityNodeInfo source = accessibilityEvent.getSource();

        if(source==null)return;

       /* final Thread threadA = new Thread(() -> {

        });
        threadA.start();
*/
        nodeList=new ArrayList<>();
        nodeListId=new ArrayList<>();

        NodeTransfer(source);// ok

        //TODO 此处埋坑，有一个listId获取在分析其他的时候方便一点？
        if(nodeList.size()<=0||nodeListId.size()<=0)return;


        if(AutoAccessibilityService.isDebug){
            for (int i = 0; i < nodeList.size(); i++) {
                String str = nodeList.get(i);
                Logs.d("Qianji_Analyze",i+"  "+str);
            }
        }

        String nodeListStr=nodeList.toString();

        Logs.d(nodeListStr);
        
        if(packageName.equals("com.tencent.mm")){


            if(nodeListId.get(0)!=null&&nodeListId.get(0).equals("com.tencent.mm:id/gas")){
                String shopName= nodeList.get(0);
                if(Caches.getOne("shopName","0")==null)
                    Caches.add("shopName",shopName,"0");
                else{
                    Caches.update("shopName",shopName);
                }
            }

            //微信转账付款
            if(findHook(".*修改.*",nodeListStr,nodeList.size(),new int[]{1,5,6})){
                Logs.d("Qianji_Analyze","=======抓取备注信息=======");
                if(AnalyzeWeChatTransfer.remark(nodeList))return;
                Logs.d("Qianji_Analyze","===============");
            }

            if(findHook("向.*转账, ￥.*, 支付方式, .*",nodeListStr,nodeList.size(),new int[]{5})){
                Logs.d("Qianji_Analyze","=======抓取支付账户信息=======");
                if(AnalyzeWeChatTransfer.account(nodeList))return;
                Logs.d("Qianji_Analyze","===============");
            }

            if(findHook("支付成功, 待.*确认收款, ￥.*, 完成",nodeListStr,nodeList.size(),new int[]{4})){
                Logs.d("Qianji_Analyze","=======抓取转账支付成功信息=======");
                if(AnalyzeWeChatTransfer.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }


            //微信个人/群红包
            if(findHook("发红包, .*金额, .*, 元, .*, 红包封面, ¥.*, 塞钱进红包.*",nodeListStr,nodeList.size(),new int[]{8,9,13,14})){
                Logs.d("Qianji_Analyze","=======抓取红包备注信息=======");
                if(AnalyzeWeChatRedPackage.remark(nodeList))return;
                Logs.d("Qianji_Analyze","===============");
            }

            if(findHook(".*微信红包, ￥.*, 支付方式, .*",nodeListStr,nodeList.size(),new int[]{5})){
                Logs.d("Qianji_Analyze","=======抓取红包支付账户信息=======");
                if(AnalyzeWeChatRedPackage.account(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }


            //微信扫码付款
            if(findHook(".*付款, 付款给个人, .*, 金额, ¥, .*",nodeListStr,nodeList.size(),new int[]{7,8})){
                Logs.d("Qianji_Analyze","=======抓取付款备注信息=======");
                if(AnalyzeWeChatPayPerson.remark(nodeList))return;
                Logs.d("Qianji_Analyze","===============");
            }
            if(findHook(".*付款给.*, ￥.*, 支付方式,.*",nodeListStr,nodeList.size(),new int[]{5})){
                Logs.d("Qianji_Analyze","=======抓取付款账户信息=======");
                if(AnalyzeWeChatPayPerson.account(nodeList))return;
                Logs.d("Qianji_Analyze","===============");
            }
            if(findHook("支付成功, ¥, .*, 收款方, .*, 完成",nodeListStr,nodeList.size(),new int[]{6})){
                Logs.d("Qianji_Analyze","=======抓取付款完成信息=======");
                if(AnalyzeWeChatPayPerson.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }

            //微信转账收款
            if(findHook("你已收款, ¥.*, 零钱余额, 零钱通 七日年化2.45%, 转入零钱通 省心赚收益, 转入, 转账时间：.*, 收款时间：.*",nodeListStr,nodeList.size(),new int[]{8})){
                Logs.d("Qianji_Analyze","=======抓取转账收款金额信息=======");
                if(AnalyzeWeChatTransferRec.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }
            //微信红包收款
            if(findHook(".*的红包, .*, 元, 已存入零钱，可直接.*, 回复表情到聊天, .*",nodeListStr,nodeList.size(),new int[]{9,14,16})){
                Logs.d("Qianji_Analyze","=======抓取转账收款金额信息=======");
                if(AnalyzeWeChatRedPackageRec.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }
            //微信扫码付款账单页面
            if(findHook(".*,   , 当前状态, 支付成功, 收款方备注, .*, 支付方式, .*, 转账时间, .*, 转账单号, .*,   , 发起群收款,   ,   , 联系收款方,   ,   , 对订单有疑惑,   ,   , 常见问题,   ,   , 取消, 账单详情, 全部账单",nodeListStr,nodeList.size(),new int[]{30})){
                Logs.d("Qianji_Analyze","=======抓取转账收款金额信息=======");
                if(AnalyzeWeChatBills.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }

        }else{

            if(nodeListId.get(0)!=null&&nodeListId.get(0).equals("com.alipay.mobile.antui:id/title_text")){
                String shopName= nodeList.get(0);
                if(Caches.getOne("alipayShopName","0")==null)
                    Caches.add("alipayShopName",shopName,"0");
                else{
                    Caches.update("alipayShopName",shopName);
                }
            }

            //支付宝部分
            //支付宝转账付款、部分红包
            if(findHook(".*, 订单信息, .*, 付款方式.*",nodeListStr,nodeList.size(),new int[]{6,7})){
                Logs.d("Qianji_Analyze","=======抓取备注信息=======");
                if(AnalyzeAlipayTransfer.remark(nodeList,getApplicationContext()))return;
                //这个地方可以抓取所有支付页面的信息
                Logs.d("Qianji_Analyze","===============");
            }
            if(findHook("转账成功, .*, 付款方式, .*, 收款方, .*, 完成",nodeListStr,nodeList.size(),new int[]{7})){
                Logs.d("Qianji_Analyze","=======抓取转账成功信息=======");
                if(AnalyzeAlipayTransfer.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }
            //支付宝红包
            if(findHook("普通红包, 发给, .*, 金额, .*, 元, 红包主题, 更多, .*, 塞钱进红包, 24小时内未被领取，红包金额将退回",nodeListStr,nodeList.size(),new int[]{7})){
                Logs.d("Qianji_Analyze","=======抓取红包备注信息=======");
                if(AnalyzeAlipayRedPackage.remark(nodeList))return;
                Logs.d("Qianji_Analyze","===============");
            }

            //支付宝扫码支付
            if(findHook(".*输入金额,  添加备注.*",nodeListStr,nodeList.size(),new int[]{6,7})){
                Logs.d("Qianji_Analyze","=======抓取支付信息=======");
                if(AnalyzeAlipayPayPerson.remark())return;
                Logs.d("Qianji_Analyze","===============");
            }

            //支付宝扫码支付成功
            if(findHook("支付成功, .*, 付款方式, .*",nodeListStr,nodeList.size(),new int[]{6,7})){
                Logs.d("Qianji_Analyze","=======抓取支付完成信息=======");
                if(AnalyzeAlipayPayPerson.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }

            //支付宝红包
            if(findHook("支付宝红包, .*, 元, 领取成功, 答谢, 查看我的红包记录, 红包编号.*",nodeListStr,nodeList.size(),new int[]{7})){
                Logs.d("Qianji_Analyze","=======抓取红包备注信息=======");
                if(AnalyzeAlipayRedPackageRec.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }

            //支付宝账单
            if(findHook(".*, 交易成功, 收款方式, .*, 转账备注, .*, 对方账户, .*, 创建时间, .*, 更多, 账单分类, .*, 标签和备注, 添加, 查看往来记录, 对此订单有疑问",nodeListStr,nodeList.size(),new int[]{18})){
                Logs.d("Qianji_Analyze","=======抓取红包备注信息=======");
                if(AnalyzeAlipayTransferRec.succeed(nodeList,getApplicationContext()))return;
                Logs.d("Qianji_Analyze","===============");
            }

        }




        Logs.d("Qianji_Screen","------------End-----------" );
    }

    private void NodeTransfer(AccessibilityNodeInfo accessibilityNodeInfo) {

        for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
            if (child != null && child.getChildCount() > 0) {
                NodeTransfer(child);
            } else if (child != null && !TextUtils.isEmpty(child.getText())) {
               Logs.d("Qianji_Node", "nodeInfo:" + child.getText()+"  className: "+child.getClassName() +" viewId "+child.getViewIdResourceName());
                nodeList.add(child.getText().toString());
                nodeListId.add(child.getViewIdResourceName());
            }
        }
    }



    private boolean findHook(String content, String nodeListStr,int nodeSize,int[] listSize){
        boolean find=false;
        for (int value : listSize) {
            if (nodeSize >= value) {
                find = true;
                break;
            }
        }
        if(!find)return false;

        Logs.d("Qianji_Match","匹配文本："+nodeListStr);
        Logs.d("Qianji_Match","匹配规则："+content);

        if(!Regex.isMatch(nodeListStr,content)) return false;

        Logs.d("Qianji_Match","匹配成功！");
        return true;
    }

    public void onServiceConnected() {

        super.onServiceConnected();
        isStart=true;
        Logs.i("辅助功能已启用");
        ServerManger.startAutoNotify(getApplicationContext());
        ServerManger.startSms(getApplicationContext());
        ServerManger.startNotice(getApplicationContext());
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);

    }
}