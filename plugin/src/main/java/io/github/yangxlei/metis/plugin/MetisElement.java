package io.github.yangxlei.metis.plugin;

import java.util.HashSet;

/**
 * Created by yanglei on 2017/7/22.
 */

/*
[
{key:io.github.yangxlei.xxx, value:["", ""]},
{key:io.github.yangxlei.xxx, value:["", ""]},
{key:io.github.yangxlei.xxx, value:["", ""]}
]
 */
public class MetisElement {
    public String key;
    public HashSet<String> values;
}
