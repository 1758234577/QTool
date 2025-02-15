package cc.hicore.qtool.QQMessage;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cc.hicore.LogUtils.LogUtils;
import cc.hicore.ReflectUtils.Classes;
import cc.hicore.ReflectUtils.MClass;
import cc.hicore.ReflectUtils.MField;
import cc.hicore.ReflectUtils.MMethod;
import cc.hicore.ReflectUtils.XPBridge;
import cc.hicore.Utils.FileUtils;
import cc.hicore.qtool.HookEnv;
import cc.hicore.qtool.QQManager.QQEnvUtils;
import cc.hicore.qtool.QQManager.QQGroupUtils;
import cc.hicore.qtool.XposedInit.HostInfo;
import de.robv.android.xposed.XposedBridge;

public class QQMessageUtils {
    public static Object GetMessageByTimeSeq(String uin, int istroop, long msgseq) {
        try {
            if (HookEnv.AppInterface == null) return null;
            Object MessageFacade = MMethod.CallMethodNoParam(HookEnv.AppInterface, "getMessageFacade",
                    MClass.loadClass("com.tencent.imcore.message.QQMessageFacade"));
            return MMethod.CallMethod(MessageFacade, "c", MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"), new Class[]{
                    String.class, int.class, long.class
            }, uin, istroop, msgseq);
        } catch (Exception e) {
            LogUtils.error("QQMessageUtils", "GetMessageByTimeSeq error:\n" + e);
            return null;
        }
    }

    public static void revokeMsg(Object msg) {
        try {
            Object MessageFacade = MMethod.CallMethodNoParam(HookEnv.AppInterface, "getMessageFacade",
                    MClass.loadClass("com.tencent.imcore.message.QQMessageFacade"));
            if (msg.getClass().toString().contains("MessageForTroopFile")) {
                RevokeTroopFile(msg);
            }

            Object MsgCache = MMethod.CallMethodNoParam(HookEnv.AppInterface, "getMsgCache",
                    MClass.loadClass("com.tencent.mobileqq.service.message.MessageCache"));

            MMethod.CallMethod(MsgCache, "b", void.class, new Class[]{boolean.class}, true);
            MessageFacade_RevokeMessage().invoke(MessageFacade, msg);
        } catch (Exception e) {
            LogUtils.error("revokeMsg", e);
        }

    }

    public static void AddMsg(Object MessageRecord) {
        try {
            Method InvokeMethod = MMethod.FindMethod("com.tencent.imcore.message.BaseQQMessageFacade", "a", void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),
                    String.class
            });
            Object MessageFacade = MMethod.CallMethodNoParam(QQEnvUtils.getAppRuntime(), "getMessageFacade",
                    MClass.loadClass("com.tencent.imcore.message.QQMessageFacade"));
            InvokeMethod.invoke(MessageFacade, MessageRecord, QQEnvUtils.getCurrentUin());
        } catch (Throwable th) {
            LogUtils.error("AddMsg", th);
        }
    }

    public static void AddAndSendMsg(Object MessageRecord) {
        try {
            Object MessageFacade = MMethod.CallMethodNoParam(QQEnvUtils.getAppRuntime(), "getMessageFacade",
                    MClass.loadClass("com.tencent.imcore.message.QQMessageFacade"));
            Method mMethod = MMethod.FindMethod("com.tencent.imcore.message.BaseQQMessageFacade", "a", void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),
                    MClass.loadClass("com.tencent.mobileqq.app.BusinessObserver")
            });
            mMethod.invoke(MessageFacade, MessageRecord, null);
        } catch (Exception e) {
            LogUtils.error("AddAndSendMsg", e);
        }

    }

    private static void RevokeTroopFile(Object MessageRecord) {
        try {
            Object RevokeHelper = QQEnvUtils.GetRevokeHelper();
            MMethod.CallMethod(RevokeHelper, "a", void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.data.MessageForTroopFile")
            }, MessageRecord);
        } catch (Exception ex) {
            LogUtils.error("RevokeTroopFile", ex);
        }
    }

    private static Method MessageFacade_RevokeMessage() {
        Method m =
                HostInfo.getVerCode() < 5670 ?
                        MMethod.FindMethod("com.tencent.imcore.message.QQMessageFacade", "d", void.class, new Class[]{Classes.MessageRecord()}) :
                        MMethod.FindMethod("com.tencent.imcore.message.QQMessageFacade", "f", void.class, new Class[]{Classes.MessageRecord()});
        return m;
    }

    public static String getCardMsg(Object msg) {
        try {
            String clzName = msg.getClass().getSimpleName();
            if (clzName.equalsIgnoreCase("MessageForStructing")) {
                Object Structing = MField.GetField(msg, "structingMsg", MClass.loadClass("com.tencent.mobileqq.structmsg.AbsStructMsg"));
                String xml = MMethod.CallMethodNoParam(Structing, "getXml", String.class);
                return xml;
            }
            if (clzName.equalsIgnoreCase("MessageForArkApp")) {
                Object ArkAppMsg = MField.GetField(msg, "ark_app_message", MClass.loadClass("com.tencent.mobileqq.data.ArkAppMessage"));
                String json = MMethod.CallMethodNoParam(ArkAppMsg, "toAppXml", String.class);
                return json;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static int DecodeAntEmoCode(int EmoCode) {
        try {
            String s = FileUtils.ReadFileString(HookEnv.AppContext.getFilesDir() + "/qq_emoticon_res/face_config.json");
            JSONObject j = new JSONObject(s);
            JSONArray arr = j.getJSONArray("sysface");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.has("AniStickerType")) {
                    if (obj.optString("QSid").equals(EmoCode + "")) {
                        String sId = obj.getString("AQLid");
                        return (Integer.parseInt(sId));
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    private static final String fakeMsgXML = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><msg serviceID=\"35\" templateID=\"1\" action=\"viewMultiMsg\" brief=\"[聊天记录]\" tSum=\"1\" sourceMsgId=\"0\" url=\"\" flag=\"3\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"1\" advertiser_id=\"0\" aid=\"0\"><title size=\"34\" maxLines=\"2\" lineSpace=\"12\">聊天记录</title><title size=\"26\" color=\"#777777\" maxLines=\"2\" lineSpace=\"12\">新消息</title><hr hidden=\"false\" style=\"0\" /><summary size=\"26\" color=\"#777777\">查看1条转发消息</summary></item><source name=\"聊天记录\" icon=\"\" action=\"\" appid=\"-1\" /></msg>";
    private static final String replaceXML = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><msg serviceID=\"35\" templateID=\"1\" action=\"viewMultiMsg\" brief=\"[聊天记录]\" m_resid=\"REPLACE\" m_fileName=\"587781278678697\" tSum=\"1\" sourceMsgId=\"0\" url=\"\" flag=\"3\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"1\" advertiser_id=\"0\" aid=\"0\"><title size=\"34\" maxLines=\"2\" lineSpace=\"12\">聊天记录</title><title size=\"26\" color=\"#777777\" maxLines=\"2\" lineSpace=\"12\">新消息</title><hr hidden=\"false\" style=\"0\" /><summary size=\"26\" color=\"#777777\">查看1条转发消息</summary></item><source name=\"聊天记录\" icon=\"\" action=\"\" appid=\"-1\" /></msg>";
    public static void sendFakeMultiMsg(String fakeGroup,String fakeUin,List messageRecords,Object session,String ShowTag,String fakeName){
        try{
            Object multiRequest = MClass.NewInstance(MClass.loadClass("com.tencent.mobileqq.multimsg.MultiMsgRequest"));
            Object struct = QQMsgBuilder.build_struct(fakeMsgXML);
            Object structContainer = QQMsgBuilder.build_MessageForStruct(struct,QQSessionUtils.Build_SessionInfo(fakeGroup,fakeUin));
            MField.SetField(multiRequest,"d",struct);
            MField.SetField(multiRequest,"e",structContainer);

            HashMap<String,String> uinContainer = new HashMap<>();
            if (!TextUtils.isEmpty(fakeName)){
                uinContainer.put(fakeUin, fakeName);
            }else {
                uinContainer.put(fakeUin, QQGroupUtils.Group_Get_Member_Name(fakeGroup,fakeUin));
            }

            MField.SetField(multiRequest,"c",uinContainer);

            List chatMessageContainer = new ArrayList();
            chatMessageContainer.addAll(messageRecords);
            MField.SetField(multiRequest,"b",chatMessageContainer);
            MField.SetField(multiRequest,"a",session);

            Object controller = MMethod.CallMethodNoParam(HookEnv.AppInterface,"getMultiMsgController",MClass.loadClass("com.tencent.mobileqq.multimsg.MultiMsgController"));
            XPBridge.HookBeforeOnce(MMethod.FindMethod(MClass.loadClass("com.tencent.mobileqq.multimsg.MultiMsgController"),"b",void.class,new Class[]{MClass.loadClass("com.tencent.mobileqq.pic.UpCallBack$SendResult")}),param -> {
                Object result = param.args[0];
                param.setResult(null);
                int code = MField.GetField(result,"a",int.class);
                if (code == 0){
                    String resid = MField.GetField(result,"f",String.class);
                    String willSendResult = replaceXML.replace("REPLACE",resid);
                    if (!TextUtils.isEmpty(ShowTag)){
                        willSendResult = willSendResult.replace("新消息",ShowTag);
                    }
                    QQMsgSender.sendStruct(session,QQMsgBuilder.build_struct(willSendResult));
                }
            });
            MMethod.CallMethodSingle(controller,"e",void.class,multiRequest);
        }catch (Exception e){
            LogUtils.error("sendFakeMultiMgs",e);
        }



    }

}
