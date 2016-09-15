package projekt.substratum.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import projekt.substratum.R;

/**
 * Created by snowpuppet on 15/09/16.
 */
public class TroubleshootingAdapter extends BaseAdapter {

    int[] tQues;
    int[] tAns;
    Context context;

    // Views
    TextView questionsTextView;
    TextView answersTextView;

    public TroubleshootingAdapter(int[] tQues, int[] tAns, Context context) {
        this.tQues = tQues;
        this.tAns = tAns;
        this.context = context;
    }

    @Override
    public int getCount() {
        return tQues.length;
    }

    @Override
    public Object getItem(int i) {
        return tQues[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.troubleshooting_row, viewGroup, false);

        questionsTextView = (TextView) v.findViewById(R.id.trouble_ques);
        answersTextView = (TextView) v.findViewById(R.id.trouble_ans);

        String question = getStringFromResource(tQues[i]);
        String answer = getStringFromResource(tAns[i]);

        questionsTextView.setText(question);
        answersTextView.setText(answer);

        return v;
    }

    private String getStringFromResource(int stringId) {
        return context.getResources().getString(stringId);
    }
}
