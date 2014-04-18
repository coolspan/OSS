package com.example.oss;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.android.oss.task.PutObjectTask;

public class MainActivity extends ActionBarActivity {
	static TextView tv_url;
	static Button bt_select;
	static Button bt_upload;
	static ImageView iv_show;
	static Handler handler;
	static Cursor cursor;

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				int id = msg.what;
				switch (id) {
				case 0:
					Toast.makeText(MainActivity.this, "上传文件失败",
							Toast.LENGTH_SHORT).show();
					break;
				case 1:
					SpannableStringBuilder stringBuilder = highlight(tv_url
							.getText().toString(), Color.BLUE, Color.GREEN, 12);
					tv_url.setText(stringBuilder);
					Toast.makeText(MainActivity.this, "上传文件成功",
							Toast.LENGTH_SHORT).show();
					break;
				case 2:
					Toast.makeText(MainActivity.this, "请选择文件",
							Toast.LENGTH_SHORT).show();
					break;

				default:
					break;
				}
			}
		};
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			tv_url = (TextView) rootView.findViewById(R.id.tv_url);
			bt_select = (Button) rootView.findViewById(R.id.bt_select);
			bt_upload = (Button) rootView.findViewById(R.id.bt_upload);
			iv_show = (ImageView) rootView.findViewById(R.id.iv_show);
			bt_select.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Intent selectFromGallery = new Intent(Intent.ACTION_PICK,
							MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(selectFromGallery, 0);
				}
			});

			bt_upload.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (cursor != null) {
						uploadFile();
					} else {
						handler.sendEmptyMessage(2);
					}
				}
			});
			return rootView;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Uri selectedImage = data.getData();
		Uri uri = Uri.parse("content://media" + selectedImage.getPath());
		ContentResolver cr = this.getContentResolver();
		cursor = cr.query(uri, null, null, null, null);
		cursor.moveToFirst();
		File file = new File(cursor.getString(1));
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Bitmap bitmap = BitmapFactory.decodeStream(fis);
		iv_show.setImageBitmap(bitmap);
		try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// for (int i = 0; i < cursor.getColumnCount(); i++) {
		// System.out.println(i + "-----------------" + cursor.getString(i));
		// }
		tv_url.setText(cursor.getString(1));
	}

	static void uploadFile() {
		new Thread(new Runnable() {

			@SuppressLint("DefaultLocale")
			@Override
			public void run() {
				md5AndSha1 md5AndSha1 = getFileMD5(new File(cursor.getString(1)));
				String ACCESS_ID = "u39NioN3ulOE09Ux";
				String ACCESS_KEY = "9SO8xw6rsiYBF1HwLkHPgt3d48a3Aj";
				String imageName = cursor.getString(3);
				String lastName = imageName.substring(imageName
						.lastIndexOf("."));
				PutObjectTask task = new PutObjectTask("wxgs", md5AndSha1.sha1
						+ lastName, cursor.getString(4));
				task.initKey(ACCESS_ID, ACCESS_KEY);
				task.setData(md5AndSha1.data);
				String result = task.getResult();
				if (md5AndSha1.md5.equals(result.toLowerCase())) {
					handler.sendEmptyMessage(1);
				} else {
					handler.sendEmptyMessage(0);
				}
				cursor = null;

			}
		}).start();

	}

	public SpannableStringBuilder highlight(String text, int color1,
			int color2, int fontSize) {
		SpannableStringBuilder spannable = new SpannableStringBuilder(text);// 用于可变字符串
		CharacterStyle span_0 = null, span_1 = null, span_2;
		int end = text.indexOf("\n");
		if (end == -1) {// 如果没有换行符就使用第一种颜色显示
			span_0 = new ForegroundColorSpan(color1);
			spannable.setSpan(span_0, 0, text.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			span_0 = new ForegroundColorSpan(color1);
			span_1 = new ForegroundColorSpan(color2);
			spannable.setSpan(span_0, 0, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			spannable.setSpan(span_1, end + 1, text.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			span_2 = new AbsoluteSizeSpan(fontSize);// 字体大小
			spannable.setSpan(span_2, end + 1, text.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return spannable;
	}

	static class md5AndSha1 {
		public String md5;
		public String sha1;
		public byte[] data;

		md5AndSha1(String md5, String sha1, byte[] data) {
			this.md5 = md5;
			this.sha1 = sha1;
			this.data = data;
		}
	}

	@SuppressLint("DefaultLocale")
	public static md5AndSha1 getFileMD5(File file) {
		if (!file.isFile()) {
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] data;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
				bos.write(buffer, 0, len);
			}
			bos.flush();
			data = bos.toByteArray();
			bos.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		String sha1 = new SHA1().getDigestOfString(data).toLowerCase();
		BigInteger bigInt = new BigInteger(1, digest.digest());
		String md5 = bigInt.toString(16).toLowerCase();
		md5AndSha1 md5AndSha1 = new md5AndSha1(md5, sha1, data);
		return md5AndSha1;
	}

	public static byte[] toByteArray(FileInputStream in) {
		byte buffer[] = new byte[1024];
		int len;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] data;
		try {
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				bos.write(buffer, 0, len);
			}
			bos.flush();
			data = bos.toByteArray();
			bos.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return data;
	}
}
