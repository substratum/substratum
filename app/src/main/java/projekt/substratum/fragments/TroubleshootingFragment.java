package projekt.substratum.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import projekt.substratum.R;
import projekt.substratum.adapters.TroubleshootingAdapter;

public class TroubleshootingFragment extends Fragment {

    ListView troubleshootListView;

    int[] troubleshootQuestions = {R.string.question_one, R.string
            .question_two, R.string.question_three, R.string.question_four, R
            .string.question_five, R.string.question_six, R.string
            .question_seven, R.string.question_eight, R.string.question_nine,
            R.string.question_ten, R.string.question_eleven, R.string
            .question_twelve};

    int[] troubleshootAnswers = {R.string.answer_one, R.string.answer_two, R
            .string.answer_three, R.string.answer_four, R.string.answer_five,
            R.string.answer_six, R.string.answer_seven, R.string
            .answer_eight, R.string.answer_nine, R.string.answer_ten, R
            .string.answer_eleven, R.string.answer_twelve};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root = inflater.inflate(R.layout.troubleshooting_fragment, container, false);

        troubleshootListView = (ListView) root.findViewById(R.id
                .troubleshoot_list_view);

        // Make sure troubleshootQuestions & troubleshootAnswers are of same
        // length and then assign adapter to listView
        troubleshootListView.setAdapter(new TroubleshootingAdapter
                (troubleshootQuestions, troubleshootAnswers, getActivity()
                        .getApplicationContext()));

        return root;
    }
}