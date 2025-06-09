use std::os::raw::{c_char};
use std::collections::HashMap;
use serde::{Deserialize, Serialize};
use chrono::NaiveDateTime;

mod abi;

pub fn main() {
}

#[derive(Deserialize, Serialize, Debug)]
struct Request {
  request_id: String,
  request_path: String,
  request_method: String,
  request_query: HashMap<String, String>,
  error_code: i32,
  data: String,
}

#[no_mangle]
pub extern fn validate(name: *mut c_char) -> *mut c_char {
  // Fetch the string from the ptr passed to the function
  let in_param = abi::string_from_ptr(name);

  // Parse the JSON string
  let mut r: Request = serde_json::from_str(&in_param).unwrap();
  println!("Parsed JSON: {:?}", r.request_id);
  println!("{:#?}", r);

  if !r.request_query.contains_key("origin") {
    // If the query contains an error, return an error message
    let error_message = format!("Error: you must specify the 'origin' query parameter",);
    r.data = error_message.clone();
    r.error_code = 400;

    let ret = request_to_string(&r);
    return abi::ptr_from_string(ret);
  } else if r.request_query.get("origin").unwrap().len() != 3 {
    let error_message = format!("Error: 'origin' must be a three character airport code",);
    r.data = error_message.clone();
    r.error_code = 400;
  
    let ret = request_to_string(&r);
    return abi::ptr_from_string(ret);
  }
  
  if !r.request_query.contains_key("destination") {
    // If the query contains an error, return an error message
    let error_message = format!("Error: you must specify the 'destination' query parameter",);
    r.data = error_message.clone();
    r.error_code = 400;
    
    let ret = request_to_string(&r);
    return abi::ptr_from_string(ret);
  } else if r.request_query.get("destination").unwrap().len() != 3 {
    let error_message = format!("Error: 'destination' must be a three character airport code",);
    r.data = error_message.clone();
    r.error_code = 400;
    
    let ret = request_to_string(&r);
    return abi::ptr_from_string(ret);
  }
  
  if !r.request_query.contains_key("from") {
    // If the query contains an error, return an error message
    let error_message = format!("Error: you must specify the 'from' query parameter",);
    r.data = error_message.clone();
    r.error_code = 400;
    
    let ret = request_to_string(&r);
    return abi::ptr_from_string(ret);
  } else {
    // validate the date
    let date = r.request_query.get("from").unwrap();
    println!("Date: {}", date);
    match NaiveDateTime::parse_from_str(date, "%Y-%m-%d %H:%M:%S") {
      Ok(parsed_date) => {
        println!("Parsed date: {}", parsed_date);
      },
      Err(e) => {
        let error_message = format!("Error: 'from' must be a valid date in the format YYYY-MM-DD HH:MM:SS: {:?}", e);
        r.data = error_message.clone();
        r.error_code = 400;
        
        let ret = request_to_string(&r);
        return abi::ptr_from_string(ret);
      }
    }
  }
  
  if !r.request_query.contains_key("to") {
    // If the query contains an error, return an error message
    let error_message = format!("Error: you must specify the 'to' query parameter",);
    r.data = error_message.clone();
    r.error_code = 400;
    
    let ret = request_to_string(&r);
    return abi::ptr_from_string(ret);
  } else {
    // validate the date
    let date = r.request_query.get("to").unwrap();
    println!("Date: {}", date);
    match NaiveDateTime::parse_from_str(date, "%Y-%m-%d %H:%M:%S") {
      Ok(parsed_date) => {
        println!("Parsed date: {}", parsed_date);
      },
      Err(e) => {
        let error_message = format!("Error: 'to' must be a valid date in the format YYYY-MM-DD HH:MM:SS: {:?}", e);
        r.data = error_message.clone();
        r.error_code = 400;
        
        let ret = request_to_string(&r);
        return abi::ptr_from_string(ret);
      }
    }
  }

  return abi::ptr_from_string(in_param);
}

fn request_to_string(request: &Request) -> String {
  // Convert the Request struct to a JSON string
  serde_json::to_string(request).unwrap()
}