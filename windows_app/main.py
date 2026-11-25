import json, csv, os, sys
from datetime import date
from pathlib import Path
import tkinter as tk
from tkinter import ttk, messagebox, filedialog

APP = "ZAKARY GRD"
TYPES = ["بلاستيك", "ورق وكرتون", "معدن", "نفايات عضوية", "نفايات كيميائية", "زيوت مستعملة", "حمأة", "أخرى"]
DEPARTMENTS = ["الإنتاج", "المخازن", "الصيانة", "المخبر", "معالجة المياه", "الإدارة", "أخرى"]
DESTINATIONS = ["إعادة التدوير", "التثمين", "المعالجة", "التخلص النهائي", "مخزن مؤقت", "أخرى"]

def data_file():
    base = Path(os.getenv("APPDATA", str(Path.home()))) / "ZAKARY-GRD"
    base.mkdir(parents=True, exist_ok=True)
    return base / "records.json"

class App:
    def __init__(self, root):
        self.root = root
        self.root.title("ZAKARY GRD - نظام تسيير واسترجاع النفايات")
        self.root.geometry("1120x760")
        self.root.minsize(900, 620)
        self.root.configure(bg="#f1f6f5")
        self.path = data_file()
        self.records = self.load()
        self.current = None
        self.style = ttk.Style()
        try: self.style.theme_use("clam")
        except Exception: pass
        self.style.configure("Treeview", rowheight=30, font=("Arial", 10))
        self.style.configure("Treeview.Heading", font=("Arial", 10, "bold"))
        self.header()
        self.nav = tk.Frame(root, bg="#e0e9e7")
        self.nav.pack(fill="x", padx=14, pady=(8,4))
        for label, page in [("لوحة القيادة","dashboard"),("تسجيل نفايات","form"),("السجلات","records"),("التكاليف والمداخيل","finance"),("التقارير والنسخ","reports")]:
            tk.Button(self.nav, text=label, command=lambda p=page:self.show(p), bg="#dce8e5", fg="#173650", relief="flat", padx=14, pady=9, font=("Arial",10,"bold")).pack(side="right", padx=4, pady=5)
        self.body = tk.Frame(root, bg="#f1f6f5")
        self.body.pack(fill="both", expand=True, padx=18, pady=12)
        self.show("dashboard")

    def header(self):
        h=tk.Frame(self.root,bg="#05090d"); h.pack(fill="x")
        tk.Label(h,text="ZAKARY GRD  ♻",bg="#05090d",fg="white",font=("Arial",22,"bold"),anchor="e",padx=20,pady=14).pack(fill="x")
        b=tk.Frame(self.root,bg="#173650"); b.pack(fill="x",padx=14,pady=(10,0))
        tk.Label(b,text="نظام تسيير واسترجاع النفايات الصناعية",bg="#173650",fg="white",font=("Arial",17,"bold"),anchor="e",padx=18,pady=10).pack(fill="x")
        tk.Label(b,text="تسجيل • متابعة • تثمين • تقارير",bg="#173650",fg="#d8f4ea",font=("Arial",10),anchor="e",padx=18,pady=4).pack(fill="x")

    def load(self):
        try: return json.loads(self.path.read_text(encoding="utf-8"))
        except Exception: return []
    def save(self):
        self.path.write_text(json.dumps(self.records,ensure_ascii=False,indent=2),encoding="utf-8")
    def clear(self):
        for w in self.body.winfo_children(): w.destroy()
    def title(self,t):
        tk.Label(self.body,text=t,bg="#f1f6f5",fg="#173650",font=("Arial",18,"bold"),anchor="e").pack(fill="x",pady=(2,12))
    def btn(self,parent,text,cmd,primary=False):
        return tk.Button(parent,text=text,command=cmd,bg="#247a5a" if primary else "#e0e9e7",fg="white" if primary else "#173650",relief="flat",font=("Arial",10,"bold"),padx=14,pady=8)
    def metric(self,parent,label,value):
        f=tk.Frame(parent,bg="white",highlightbackground="#dce7e4",highlightthickness=1)
        tk.Label(f,text=label,bg="white",fg="#687b89",font=("Arial",10),anchor="e",padx=12,pady=7).pack(fill="x")
        tk.Label(f,text=value,bg="white",fg="#247a5a",font=("Arial",18,"bold"),anchor="e",padx=12,pady=5).pack(fill="x")
        return f
    def show(self,page):
        self.clear()
        if page=="dashboard": self.dashboard()
        elif page=="form": self.form()
        elif page=="records": self.records_page()
        elif page=="finance": self.finance()
        else: self.reports()
    def totals(self, subset=None):
        rs=self.records if subset is None else subset
        return (sum(float(x["quantity"]) for x in rs),sum(float(x["recovered"]) for x in rs),sum(float(x["cost"]) for x in rs),sum(float(x["income"]) for x in rs))
    def dashboard(self):
        self.title("لوحة القيادة")
        q,r,c,i=self.totals()
        grid=tk.Frame(self.body,bg="#f1f6f5");grid.pack(fill="x")
        vals=[("إجمالي النفايات المنتجة",f"{q:.2f} كغ"),("إجمالي الكمية المسترجعة",f"{r:.2f} كغ"),("المعالجة / التخلص",f"{max(q-r,0):.2f} كغ"),("نسبة الاسترجاع",f"{(r*100/q if q else 0):.1f}%"),("صافي التكاليف",f"{c-i:.2f} دج"),("عدد السجلات",str(len(self.records)))]
        for n,(a,b) in enumerate(vals):
            x=self.metric(grid,a,b);x.grid(row=n//3,column=n%3,sticky="nsew",padx=5,pady=5)
        for col in range(3): grid.columnconfigure(col,weight=1)
        self.title("ملخص حسب نوع النفايات")
        for typ in TYPES:
            s=[x for x in self.records if x["type"]==typ]
            if s:
                tq,tr,tc,ti=self.totals(s)
                tk.Label(self.body,text=f"♻ {typ}  |  منتج: {tq:.2f} كغ  |  مسترجع: {tr:.2f} كغ  |  التكاليف: {tc:.2f} دج  |  المداخيل: {ti:.2f} دج  |  الصافي: {tc-ti:.2f} دج",bg="white",fg="#243746",font=("Arial",10),anchor="e",padx=12,pady=8).pack(fill="x",pady=2)
    def form(self, rec=None):
        self.clear(); self.title("تسجيل عملية نفايات" if rec is None else "تعديل عملية نفايات")
        self.editing=rec
        box=tk.Frame(self.body,bg="#f1f6f5");box.pack(fill="x")
        self.vars={}
        def entry(key,label,default=""):
            row=tk.Frame(box,bg="#f1f6f5");row.pack(fill="x",pady=4)
            tk.Label(row,text=label,width=31,anchor="e",bg="#f1f6f5",font=("Arial",10)).pack(side="right",padx=8)
            v=tk.StringVar(value=str(default));self.vars[key]=v
            tk.Entry(row,textvariable=v,justify="right",font=("Arial",11)).pack(side="right",fill="x",expand=True,ipady=6)
        def combo(key,label,values,default=None):
            row=tk.Frame(box,bg="#f1f6f5");row.pack(fill="x",pady=4)
            tk.Label(row,text=label,width=31,anchor="e",bg="#f1f6f5",font=("Arial",10)).pack(side="right",padx=8)
            v=tk.StringVar(value=default or values[0]);self.vars[key]=v
            ttk.Combobox(row,textvariable=v,values=values,state="readonly",justify="right",font=("Arial",11)).pack(side="right",fill="x",expand=True,ipady=4)
        d=rec or {}
        entry("date","التاريخ (YYYY-MM-DD)",d.get("date",date.today().isoformat()))
        combo("type","نوع النفايات",TYPES,d.get("type"))
        combo("department","القسم المنتج",DEPARTMENTS,d.get("department"))
        entry("quantity","الكمية الإجمالية بالكيلوغرام",d.get("quantity",""))
        entry("recovered","الكمية المسترجعة بالكيلوغرام",d.get("recovered",""))
        combo("destination","وجهة النفايات",DESTINATIONS,d.get("destination"))
        entry("cost","تكلفة المعالجة أو النقل (دج)",d.get("cost",0))
        entry("income","مداخيل التثمين أو البيع (دج)",d.get("income",0))
        entry("note","ملاحظات أو رقم وصل",d.get("note",""))
        row=tk.Frame(self.body,bg="#f1f6f5");row.pack(anchor="e",pady=15)
        self.btn(row,"حفظ العملية",self.save_form,True).pack(side="right",padx=5)
        self.btn(row,"إلغاء",lambda:self.show("dashboard")).pack(side="right",padx=5)
    def save_form(self):
        try:
            d={k:v.get().strip() for k,v in self.vars.items()}
            q=float(d["quantity"].replace(",","."));r=float(d["recovered"].replace(",","."))
            c=float(d["cost"].replace(",","."));i=float(d["income"].replace(",","."))
            if not d["date"] or q<=0 or r<0 or r>q or c<0 or i<0: raise ValueError()
            for k,v in [("quantity",q),("recovered",r),("cost",c),("income",i)]: d[k]=v
            if self.editing is not None:
                d["id"]=self.editing["id"];idx=next(n for n,x in enumerate(self.records) if x["id"]==d["id"]);self.records[idx]=d
            else:
                import time;d["id"]=int(time.time()*1000);self.records.insert(0,d)
            self.save();messagebox.showinfo(APP,"تم حفظ العملية بنجاح");self.show("dashboard")
        except Exception: messagebox.showerror(APP,"تحقق من التاريخ والكميات والتكاليف. يجب ألا تتجاوز الكمية المسترجعة الكمية الإجمالية.")
    def records_page(self):
        self.title("سجل العمليات")
        top=tk.Frame(self.body,bg="#f1f6f5");top.pack(fill="x",pady=5)
        search=tk.StringVar()
        tk.Entry(top,textvariable=search,justify="right",font=("Arial",11)).pack(side="right",fill="x",expand=True,ipady=6)
        holder=tk.Frame(self.body);holder.pack(fill="both",expand=True,pady=8)
        cols=("date","type","department","quantity","recovered","destination","cost","income")
        tree=ttk.Treeview(holder,columns=cols,show="headings")
        heads=["التاريخ","النوع","القسم","الإجمالي كغ","المسترجع كغ","الوجهة","التكلفة دج","المداخيل دج"]
        for col,head in zip(cols,heads): tree.heading(col,text=head);tree.column(col,width=115,anchor="center")
        tree.pack(fill="both",expand=True)
        def refresh():
            tree.delete(*tree.get_children())
            q=search.get().lower()
            for x in self.records:
                if not q or q in " ".join(str(x.get(k,"")) for k in ["date","type","department","destination","note"]).lower():
                    tree.insert("","end",iid=str(x["id"]),values=tuple(x.get(k,"") for k in cols))
        def selected():
            s=tree.selection()
            if not s: messagebox.showwarning(APP,"اختر سجلاً أولاً");return None
            return next((x for x in self.records if str(x["id"])==s[0]),None)
        actions=tk.Frame(self.body,bg="#f1f6f5");actions.pack(anchor="e")
        self.btn(actions,"بحث / تحديث",refresh).pack(side="right",padx=4)
        self.btn(actions,"تعديل",lambda:(self.form(selected()) if selected() else None)).pack(side="right",padx=4)
        def delete():
            x=selected()
            if x and messagebox.askyesno(APP,"هل تريد حذف السجل المحدد؟"):
                self.records=[z for z in self.records if z["id"]!=x["id"]];self.save();refresh()
        self.btn(actions,"حذف",delete).pack(side="right",padx=4)
        refresh()
    def finance(self):
        self.clear();self.title("التكاليف والمداخيل")
        q,r,c,i=self.totals()
        for a,b in [("إجمالي تكاليف المعالجة والنقل",c),("إجمالي مداخيل التثمين والبيع",i),("صافي التكاليف",c-i)]:
            self.metric(self.body,a,f"{b:.2f} دج").pack(fill="x",pady=4)
        self.title("تفصيل مالي مستقل لكل نوع نفايات")
        for typ in TYPES:
            s=[x for x in self.records if x["type"]==typ]
            if s:
                tq,tr,tc,ti=self.totals(s)
                text=f"{typ}  |  المنتج: {tq:.2f} كغ  |  المسترجع: {tr:.2f} كغ  |  التكاليف: {tc:.2f} دج  |  المداخيل: {ti:.2f} دج  |  الصافي: {tc-ti:.2f} دج  |  تكلفة الكغ المنتج: {tc/tq if tq else 0:.2f} دج"
                tk.Label(self.body,text=text,bg="white",fg="#243746",anchor="e",font=("Arial",10),padx=12,pady=10).pack(fill="x",pady=3)
        tk.Label(self.body,text="تُحسب تكلفة كل نوع وحده من العمليات المسجلة لذلك النوع، ولا تُوزَّع تكلفة نوع على الأنواع الأخرى.",bg="#fff2cc",fg="#795500",anchor="e",padx=10,pady=10).pack(fill="x",pady=8)
    def reports(self):
        self.clear();self.title("التقارير والنسخ الاحتياطي")
        self.btn(self.body,"تصدير تقرير CSV",self.export_csv,True).pack(anchor="e",pady=6)
        self.btn(self.body,"حفظ نسخة احتياطية JSON",self.export_json).pack(anchor="e",pady=6)
        self.btn(self.body,"استيراد نسخة احتياطية JSON",self.import_json).pack(anchor="e",pady=6)
        self.btn(self.body,"عرض ملخص حسب نوع النفايات",self.summary).pack(anchor="e",pady=6)
        tk.Label(self.body,text=f"مكان حفظ البيانات تلقائياً: {self.path}",bg="#fff2cc",fg="#795500",anchor="e",wraplength=900,padx=10,pady=12).pack(fill="x",pady=12)
    def export_csv(self):
        p=filedialog.asksaveasfilename(defaultextension=".csv",filetypes=[("CSV","*.csv")],initialfile="ZAKARY-GRD-report.csv")
        if not p:return
        with open(p,"w",newline="",encoding="utf-8-sig") as f:
            w=csv.writer(f);w.writerow(["Date","Type","Department","Quantity_kg","Recovered_kg","Destination","Cost_DZD","Income_DZD","Notes"])
            for x in self.records:w.writerow([x.get(k,"") for k in ["date","type","department","quantity","recovered","destination","cost","income","note"]])
        messagebox.showinfo(APP,"تم تصدير التقرير.")
    def export_json(self):
        p=filedialog.asksaveasfilename(defaultextension=".json",filetypes=[("JSON","*.json")],initialfile="ZAKARY-GRD-backup.json")
        if p:
            Path(p).write_text(json.dumps(self.records,ensure_ascii=False,indent=2),encoding="utf-8");messagebox.showinfo(APP,"تم حفظ النسخة الاحتياطية.")
    def import_json(self):
        p=filedialog.askopenfilename(filetypes=[("JSON","*.json")])
        if not p:return
        try:
            arr=json.loads(Path(p).read_text(encoding="utf-8"));known={x["id"] for x in self.records};added=0
            for x in arr:
                if x.get("id") not in known:self.records.append(x);added+=1
            self.save();messagebox.showinfo(APP,f"تم استيراد {added} سجل.");self.show("dashboard")
        except Exception:messagebox.showerror(APP,"تعذر قراءة ملف النسخة الاحتياطية.")
    def summary(self):
        lines=[]
        for t in TYPES:
            s=[x for x in self.records if x["type"]==t]
            if s:
                q,r,_,_=self.totals(s);lines.append(f"{t}: إجمالي {q:.2f} كغ، مسترجع {r:.2f} كغ")
        messagebox.showinfo("ملخص أنواع النفايات","\n\n".join(lines) if lines else "لا توجد بيانات بعد.")

if __name__ == "__main__":
    root=tk.Tk();App(root);root.mainloop()
