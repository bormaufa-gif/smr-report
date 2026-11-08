package ru.smr.report;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.ArrayList;

public class VoiceActivity extends MainActivity {
 private WebView attached,target; private boolean busy; private String script;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  try(java.io.InputStream in=getAssets().open("dictation.js");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);script=out.toString("UTF-8");}catch(Exception e){notice("Диктовка недоступна: ошибка загрузки");return;}
  ViewGroup content=findViewById(android.R.id.content);
  content.getViewTreeObserver().addOnGlobalLayoutListener(()->{
   if(content.getChildCount()==0||!(content.getChildAt(0) instanceof LinearLayout))return;
   LinearLayout root=(LinearLayout)content.getChildAt(0);
   for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof WebView){WebView w=(WebView)root.getChildAt(i);if(w!=attached)attach(w);}
  });
 }
 private boolean trusted(WebView w){return w!=null&&w.isAttachedToWindow()&&w.getUrl()!=null&&w.getUrl().startsWith("https://bormaufa-gif.github.io/smr-report/");}
 private void attach(WebView w){
  attached=w;WebViewClient previous=w.getWebViewClient();
  w.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView v,String url){previous.onPageFinished(v,url);if(trusted(v))v.evaluateJavascript(script,null);}
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
    if("ptsvoice".equals(r.getUrl().getScheme())){if(r.isForMainFrame()&&r.hasGesture()&&trusted(v)&&"start".equals(r.getUrl().getHost()))begin(v);return true;}
    return previous.shouldOverrideUrlLoading(v,r);
   }
  });
  if(trusted(w))w.evaluateJavascript(script,null);
 }
 private void notice(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void clear(){if(trusted(target))target.evaluateJavascript("window.ptsDictation&&window.ptsDictation.cancel()",null);busy=false;target=null;}
 private void begin(WebView w){
  if(busy)return;busy=true;target=w;
  w.evaluateJavascript("!!(window.ptsDictation&&window.ptsDictation.ready())",r->{
   if(!"true".equals(r)){clear();return;}
   new AlertDialog.Builder(this).setTitle("Диктовка").setMessage("Системный сервис может отправлять речь на сервер провайдера. После распознавания проверьте текст. Отчёт автоматически не отправляется.").setPositiveButton("Начать",(d,b)->recognize()).setNegativeButton("Отмена",(d,b)->clear()).setOnCancelListener(d->clear()).show();
  });
 }
 private void recognize(){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);try{startActivityForResult(i,711);}catch(Exception e){clear();notice("На устройстве нет доступного сервиса диктовки. Ручной ввод сохранён.");}}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);if(request!=711)return;
  ArrayList<String> items=data==null?null:data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
  if(result!=RESULT_OK||items==null||items.isEmpty()||!trusted(target)){clear();return;}
  EditText edit=new EditText(this);edit.setInputType(131073);edit.setMinLines(3);edit.setText(items.get(0));
  new AlertDialog.Builder(this).setTitle("Проверьте текст").setMessage("Текст добавится в конец выбранного поля.").setView(edit).setPositiveButton("Добавить в поле",(d,b)->insert(edit.getText().toString())).setNegativeButton("Отмена",(d,b)->clear()).setOnCancelListener(d->clear()).show();
 }
 private void insert(String text){if(!trusted(target)||text.trim().isEmpty()){clear();return;}target.evaluateJavascript("!!(window.ptsDictation&&window.ptsDictation.insert("+JSONObject.quote(text.trim())+"))",r->{notice("true".equals(r)?"Текст добавлен. Проверьте отчёт.":"Поле изменилось или текст слишком длинный. Вставка отменена.");clear();});}
}
