package in.co.gamedev.bookexchange.common;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import in.co.gamedev.bookexchange.R;
import in.co.gamedev.bookexchange.apiclient.BookExchangeServiceAsync;
import in.co.gamedev.server.bookexchange.bookExchangeService.model.ChangeExchangeApprovalRequest;
import in.co.gamedev.server.bookexchange.bookExchangeService.model.ChangeExchangeApprovalResponse;
import in.co.gamedev.server.bookexchange.bookExchangeService.model.Exchange;

/**
 * Created by suhas on 2/26/2015.
 */
public class ExchangeListRecyclerAdapter
    extends RecyclerView.Adapter<ExchangeListRecyclerAdapter.ExchangeItemViewHolder> {

  private final List<Exchange> exchangeList;

  public ExchangeListRecyclerAdapter(List<Exchange> exchangeList) {
    this.exchangeList = exchangeList;
  }

  public void addItems(List<Exchange> books) {
    exchangeList.addAll(books);
    notifyDataSetChanged();
  }

  @Override
  public ExchangeItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.exchange_list_item, parent, false);
    final ExchangeItemViewHolder viewHolder = new ExchangeItemViewHolder(view);
    return viewHolder;
  }

  @Override
  public void onBindViewHolder(ExchangeItemViewHolder holder, int position) {
    holder.setExchange(exchangeList.get(position));
  }

  @Override
  public int getItemCount() {
    return exchangeList.size();
  }

  public class ExchangeItemViewHolder extends RecyclerView.ViewHolder {

    private ImageView pickupBookThumbnail;
    private TextView pickupBookTitle;
    private TextView pickupBookAuthor;
    private ImageView dropBookThumbnail;
    private TextView dropBookTitle;
    private TextView dropBookAuthor;
    private TextView myApprovalStatus;
    private TextView otherUserApprovalStatus;
    private TextView exchangeCompleteText;
    private Button approveButton;

    private ProgressDialog progressDialog;

    public ExchangeItemViewHolder(View itemView) {
      super(itemView);
      pickupBookThumbnail = (ImageView) itemView.findViewById(R.id.pickup_book_thumbnail);
      pickupBookTitle = (TextView) itemView.findViewById(R.id.pickup_book_title);
      pickupBookAuthor = (TextView) itemView.findViewById(R.id.pickup_book_author);

      dropBookThumbnail = (ImageView) itemView.findViewById(R.id.drop_book_thumbnail);
      dropBookTitle = (TextView) itemView.findViewById(R.id.drop_book_title);
      dropBookAuthor = (TextView) itemView.findViewById(R.id.drop_book_author);

      myApprovalStatus = (TextView) itemView.findViewById(R.id.my_approval_status);
      otherUserApprovalStatus = (TextView) itemView.findViewById(R.id.other_user_approval_status);
      approveButton = (Button) itemView.findViewById(R.id.approve_button);

      exchangeCompleteText = (TextView) itemView.findViewById(R.id.exchange_complete_text);
    }

    private void setExchange(final Exchange exchange) {
      pickupBookTitle.setText(exchange.getPickupBook().getTitle());
      pickupBookAuthor.setText(exchange.getPickupBook().getAuthor());
      new ImageLoaderTask(exchange.getPickupBook().getThumbnailUrl(), pickupBookThumbnail).execute();

      dropBookTitle.setText(exchange.getDropBook().getTitle());
      dropBookAuthor.setText(exchange.getDropBook().getAuthor());
      new ImageLoaderTask(exchange.getDropBook().getThumbnailUrl(), dropBookThumbnail).execute();

      if (exchange.getMyApprovalStatus().equals(Constants.APPROVAL_STATUS_APPROVED)
          && exchange.getOtherUserApprovalStatus().equals(Constants.APPROVAL_STATUS_APPROVED)) {
        exchangeCompleteText.setVisibility(View.VISIBLE);
      } else {
        exchangeCompleteText.setVisibility(View.GONE);
      }
      myApprovalStatus.setText(exchange.getMyApprovalStatus());
      otherUserApprovalStatus.setText(exchange.getOtherUserApprovalStatus());
      if (!exchange.getMyApprovalStatus().equals(Constants.APPROVAL_STATUS_APPROVED)) {
        approveButton.setVisibility(View.VISIBLE);
      } else {
        approveButton.setVisibility(View.GONE);
      }
      approveButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          SharedPreferences prefs = itemView.getContext().getSharedPreferences(
              Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);
          ChangeExchangeApprovalRequest request = new ChangeExchangeApprovalRequest()
              .setUserId(prefs.getString(Constants.PREF_USER_ID, null))
              .setExchangeCycleId(exchange.getExchangeCycleId())
              .setNewApprovalStatus(Constants.APPROVAL_STATUS_APPROVED);
          handleApproveButtonClick(request);
        }
      });
    }

    private void handleApproveButtonClick(ChangeExchangeApprovalRequest request) {
      new AsyncTask<ChangeExchangeApprovalRequest, Void, ChangeExchangeApprovalResponse>() {

        @Override
        protected void onPreExecute() {
          progressDialog = new ProgressDialog(itemView.getContext(), R.style.progress_dialog);
          progressDialog.setCancelable(true);
          progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
          progressDialog.show();
        }

        @Override
        protected ChangeExchangeApprovalResponse doInBackground(
            ChangeExchangeApprovalRequest... params) {
          try {
            return BookExchangeServiceAsync.getInstance().changeExchangeApproval(params[0]);
          } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onPostExecute(ChangeExchangeApprovalResponse response) {
          progressDialog.dismiss();
          approveButton.setVisibility(View.GONE);
          myApprovalStatus.setText(Constants.APPROVAL_STATUS_APPROVED);
        }
      }.execute(request);
    }
  }
}