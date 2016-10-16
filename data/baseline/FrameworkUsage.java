class UB implements B {                   
  @Override void x() {                    
    F.magic(null);                        
  }                                       
}                                         
class UA implements A {                   
  List f1;                                
  UA(F a0) {                              
    a0.set(this);                         
  }                                       
  @Override void x() {                    
    ArrayList al = new ArrayList();       
    this.f1 = al;                         
    UB ub = new UB();                     
    al.add(ub);                           
  }                                       
  @Override B y() {                       
    List l = this.f1;                     
    Object o = l.get(??);                 
    return o;                             
  }                                       
}                                         
