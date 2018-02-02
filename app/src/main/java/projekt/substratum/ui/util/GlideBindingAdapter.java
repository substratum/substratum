package projekt.substratum.ui.util;

import android.databinding.BindingAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class GlideBindingAdapter {

    @BindingAdapter("imageUrl")
    public static void imageUrl(ImageView imageView, String url){
        Glide.with(imageView.getContext())
                .load(url)
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(imageView);
    }
}