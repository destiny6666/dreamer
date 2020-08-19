package com.jyq.dreamer.data.structure;

/**
 * @ClassName: RedBlankTree
 * @description: 红黑树
 * @author: jiayuqin2
 * @create: 2020-08-19 11:12
 **/
/**
 * 红黑树性质：    1、节点非黑即红  2、两个红色节点不能相连 3、root节点为黑色 4、每个红节点的子节点都是黑色，叶子节点均为黑色
 * 所有插入的默认为红色
 * 变颜色：当父节点+叔叔节点均为红色节点=父+叔节点均置为黑色，爷爷节点置为红色
 * 左旋：父节点（红）+叔叔节点（黑）+右子树=以父节点左旋
 * 右旋：父节点（红）+叔叔节点（黑）+左子树=以祖父节点右旋（父节点->黑色，祖父节点->红色,以祖父节点右旋）
 */
public class RedBlankTree {
    private final int R=0;
    private final int B=1;
    private Node root=null;//红黑树根节点
    class Node{
        int data;
        int color=R;
        Node left;
        Node right;
        Node parent;

        public Node(int data) {
            this.data = data;
        }
    }
    //插入
    public void insert(Node root,int data){//root节点一定不为空，刚开始默认进去
        //查询右子树
        if(data>=root.data){
            if(null==root.right){
                root.right=new Node(data);
                return;
            }
            insert(root.right,data);
            return;
        }
        //查询左子树
        if (null==root.left){
            root.left=new Node(data);
            return;
        }
        insert(root.left,data);
        return;
    }
    public void leftRotate(Node node){
        //根节点
        if(null==node.parent){
            Node E=root;
            Node S=E.right;

            //移动S的左子树到E的右子树
            E.right=S.left;
            S.left.parent=E;
            //修改E的父指针
            E.parent=S;
            //修改S的父指针
            S.parent=null;
        }

    }
}
