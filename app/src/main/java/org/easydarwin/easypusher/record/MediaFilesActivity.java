package org.easydarwin.easypusher.record;

import android.content.ActivityNotFoundException;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.databinding.ActivityMediaFilesBinding;
import org.easydarwin.easypusher.databinding.FragmentMediaFileBinding;
import org.easydarwin.easypusher.databinding.ImagePickerItemBinding;
import org.easydarwin.easypusher.util.Config;

import java.io.File;
import java.io.FilenameFilter;

/**
 * 录像 / 抓拍
 * */
public class MediaFilesActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private ActivityMediaFilesBinding mDataBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_media_files);

        setSupportActionBar(mDataBinding.mainToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDataBinding.mainToolbar.setOnMenuItemClickListener(this);
        // 左边的小箭头（注意需要在setSupportActionBar(toolbar)之后才有效果）
        mDataBinding.mainToolbar.setNavigationIcon(R.drawable.com_back);

        mDataBinding.viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 1;
            }

            public Fragment getItem(int position) {
                Bundle args = new Bundle();
                args.putBoolean(LocalFileFragment.KEY_IS_RECORD, position == 0);
                return Fragment.instantiate(MediaFilesActivity.this, LocalFileFragment.class.getName(), args);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return "";
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    // 返回的功能
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class LocalFileFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
        public static final String KEY_IS_RECORD = "key_last_selection";

        private boolean mShowMp4File;
        private ActionMode mActionMode;
        private FragmentMediaFileBinding mBinding;

        SparseArray<Boolean> mImageChecked;

        private String mSuffix;
        File mRoot = null;
        File[] mSubFiles;
        int mImgHeight;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(false);

            mImageChecked = new SparseArray<>();

            mShowMp4File = getArguments().getBoolean(KEY_IS_RECORD);
            mSuffix = mShowMp4File ? ".mp4" : ".jpg";
            File easyPusher = new File(Config.recordPath());
            easyPusher.mkdir();
            mRoot = easyPusher;

            File[] subFiles = mRoot.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(mSuffix);
                }
            });

            if (subFiles == null)
                subFiles = new File[0];

            mSubFiles = subFiles;
            mImgHeight = (int) (getResources().getDisplayMetrics().density * 100 + 0.5f);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_media_file, container, false);
            return mBinding.getRoot();
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            mBinding.recycler.setLayoutManager(layoutManager);
            mBinding.recycler.setAdapter(new RecyclerView.Adapter() {
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    ImagePickerItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.image_picker_item, parent, false);
                    return new ImageItemHolder(binding);
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    ImageItemHolder holder = (ImageItemHolder) viewHolder;
                    holder.mCheckBox.setOnCheckedChangeListener(null);
                    holder.mCheckBox.setChecked(mImageChecked.get(position, false));
                    holder.mCheckBox.setOnCheckedChangeListener(LocalFileFragment.this);
                    holder.mCheckBox.setTag(R.id.click_tag, holder);
                    holder.mImage.setTag(R.id.click_tag, holder);

                    if (mShowMp4File) {
                        holder.mPlayImage.setVisibility(View.VISIBLE);
                    } else {
                        holder.mPlayImage.setVisibility(View.GONE);
                    }

                    Glide.with(getContext())
                            .load(mSubFiles[position])
                            .into(holder.mImage);
                }

                @Override
                public int getItemCount() {
                    return mSubFiles.length;
                }
            });
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ImageItemHolder holder = (ImageItemHolder) buttonView.getTag(R.id.click_tag);
            int position = holder.getAdapterPosition();

            if (mActionMode != null) {
                mActionMode.invalidate();
            }
        }

        @Override
        public void onClick(View v) {
            ImageItemHolder holder = (ImageItemHolder) v.getTag(R.id.click_tag);

            if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) {
                return;
            }

            final String path = mSubFiles[holder.getAdapterPosition()].getPath();

            if (TextUtils.isEmpty(path)) {
                Toast.makeText(getContext(), "文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            if (path.endsWith(".mp4")) {
                try {
                    File f = new File(path);
                    Uri uri =null;
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= 24) {//7.0 Android N
                        //com.xxx.xxx.fileprovider为上述manifest中provider所配置相同
                        uri = FileProvider.getUriForFile(getContext(), "org.easydarwin.easyrtmp.fileProvider",f);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//7.0以后，系统要求授予临时uri读取权限，安装完毕以后，系统会自动收回权限，该过程没有用户交互
                    } else {//7.0以下
                        uri = Uri.fromFile(f);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    intent.setDataAndType(uri, "video/*");
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
//            } else if (path.endsWith(".jpg")) {
//                try {
//                    Intent intent = new Intent();
//                    intent.setAction(Intent.ACTION_VIEW);
//
//                    Uri fileUri = FileProvider.getUriForFile(getContext(), getString(R.string.org_easydarwin_update_authorities), mSubFiles[holder.getAdapterPosition()]);
//                    intent.setDataAndType(fileUri,"image/*");
//                    startActivity(intent);
//                } catch (ActivityNotFoundException e) {
//                    e.printStackTrace();
//                }
            }
        }

        class ImageItemHolder extends RecyclerView.ViewHolder {
            public final CheckBox mCheckBox;
            public final ImageView mImage;
            public final ImageView mPlayImage;

            public ImageItemHolder(ImagePickerItemBinding binding) {
                super(binding.getRoot());

                mCheckBox = binding.imageCheckbox;
                mImage = binding.imageIcon;
                mPlayImage = binding.imagePlay;
                mImage.setOnClickListener(LocalFileFragment.this);
            }
        }
    }
}
