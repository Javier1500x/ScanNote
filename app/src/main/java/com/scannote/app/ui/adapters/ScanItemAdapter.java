package com.scannote.app.ui.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.scannote.app.R;
import com.scannote.app.database.DocumentEntry;
import java.util.ArrayList;
import java.util.List;

public class ScanItemAdapter extends RecyclerView.Adapter<ScanItemAdapter.ScanViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DocumentEntry entry);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(DocumentEntry entry);
    }

    private List<DocumentEntry> items = new ArrayList<>();
    private final OnItemClickListener clickListener;
    private final OnItemDeleteListener deleteListener;

    public ScanItemAdapter(OnItemClickListener clickListener, OnItemDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<DocumentEntry> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan, parent, false);
        return new ScanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        DocumentEntry entry = items.get(position);
        holder.bind(entry, clickListener, deleteListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ScanViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textPreview;
        final TextView textDate;
        final TextView textWordCount;
        final ImageView iconType;
        final View btnDeleteScan;

        ScanViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle     = itemView.findViewById(R.id.text_title);
            textPreview   = itemView.findViewById(R.id.text_preview);
            textDate      = itemView.findViewById(R.id.text_date);
            textWordCount = itemView.findViewById(R.id.text_word_count);
            iconType      = itemView.findViewById(R.id.icon_type);
            btnDeleteScan = itemView.findViewById(R.id.btn_delete_scan);
        }

        void bind(DocumentEntry entry, OnItemClickListener clickListener, OnItemDeleteListener deleteListener) {
            // Título
            textTitle.setText(entry.title != null && !entry.title.isEmpty()
                    ? entry.title : "Escaneo sin título");

            // Preview del contenido
            String content = entry.content != null ? entry.content.trim() : "";
            textPreview.setText(content.isEmpty() ? "Sin contenido" : content);

            // Fecha relativa
            CharSequence relTime = DateUtils.getRelativeTimeSpanString(
                    entry.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            textDate.setText(relTime);

            // Contador de palabras
            int words = content.isEmpty() ? 0 : content.split("\\s+").length;
            textWordCount.setText(words + " palabras");

            // Click
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onItemClick(entry);
            });

            // Delete
            if (btnDeleteScan != null) {
                btnDeleteScan.setOnClickListener(v -> {
                    if (deleteListener != null) deleteListener.onItemDelete(entry);
                });
            }
        }
    }
}
