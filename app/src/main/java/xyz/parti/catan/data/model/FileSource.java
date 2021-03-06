package xyz.parti.catan.data.model;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Parcel
public class FileSource {
    public Long id;
    public String attachment_url;
    public String attachment_lg_url;
    public String attachment_md_url;
    public String attachment_sm_url;
    public String name;
    public String file_type;
    public String file_size;
    public String human_file_size;
    public Integer seq_no;
    public Float image_ratio = 0.8f;

    public boolean isImage() {
        if (file_type == null) { return false; }
        return file_type.startsWith("image/");
    }

    public boolean isDoc() {
        return !isImage();
    }

    public Collection<? extends String> getPreloadImageUrls() {
        List<String> result = new ArrayList<>();
        result.add(attachment_sm_url);
        return result;
    }
}
