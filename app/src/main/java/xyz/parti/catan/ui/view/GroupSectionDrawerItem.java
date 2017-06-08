package xyz.parti.catan.ui.view;

import android.support.annotation.LayoutRes;

import com.mikepenz.materialdrawer.model.SectionDrawerItem;

import xyz.parti.catan.R;

/**
 * Created by dalikim on 2017. 6. 8..
 */

public class GroupSectionDrawerItem extends SectionDrawerItem {
    @Override
    public int getType() {
        return R.id.material_drawer_item_group;
    }

    @Override
    @LayoutRes
    public int getLayoutRes() {
        return R.layout.drawer_item_group;
    }

}
